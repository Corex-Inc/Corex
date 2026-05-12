package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.utils.scripts.CommandHelper;
import dev.corexinc.corex.environment.utils.scripts.JsonHelper;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/* @doc command
 *
 * @Name Fetch
 * @Syntax fetch [<url>] (body:<data>) (type:<json/form>) (method:<method>) (headers:<map>) (timeout:<duration>) (savefile:<path>)
 * @RequiredArgs 1
 * @MaxArgs 7
 * @Aliases webget
 * @ShortDescription Asynchronously fetches a webpage or API response.
 *
 * @Implements Fetch
 *
 * @Description
 * Connects to a webpage or API and downloads its contents asynchronously.
 *
 * You can specify the payload format using the `type:` argument:
 * - `type:json` will automatically convert a MapTag/ListTag body into a JSON string and set the `Content-Type: application/json` header.
 * - `type:form` will convert a MapTag into a URL-encoded form (e.g. key1=val1&key2=val2) and set `Content-Type: application/x-www-form-urlencoded`.
 * - If omitted, the body is sent as raw plain text.
 *
 * @Usage
 * // Send JSON to an API
 * - definemap my_data:
 *     username: Notch
 *     action: ban
 * - ~fetch "https://api.example.com/punish" body:<[my_data]> type:json method:POST save:res
 *
 * @Usage
 * // Send Form-Data (like a standard HTML form)
 * - definemap login_form:
 *     login: admin
 *     password: super#secret!password
 * - ~fetch "https://api.example.com/auth" body:<[login_form]> type:form method:POST
 */
public class FetchCommand implements AbstractCommand {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public @NonNull String getName() { return "fetch"; }

    @Override
    public @NonNull List<String> getAlias() { return List.of("webget"); }

    @Override
    public @NonNull String getSyntax() {
        return "[<url>] (body:<data>) (type:<json/form>) (method:<method>) (headers:<map>) (timeout:<duration>) (savefile:<path>)";
    }

    @Override
    public int getMinArgs() { return 1; }

    @Override
    public int getMaxArgs() { return 7; }

    @Override
    public boolean isAsyncSafe() { return true; }

    @Override
    public boolean setCanBeWaitable() { return true; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String url = instruction.getLinear(0, queue);
        AbstractTag bodyTag = instruction.getPrefixObject("body", queue);
        String payloadType = instruction.getPrefix("type", queue);
        String method = instruction.getPrefix("method", queue);
        AbstractTag headersTag = instruction.getPrefixObject("headers", queue);
        String saveFile = instruction.getPrefix("savefile", queue);
        AbstractTag timeoutTag = instruction.getPrefixObject("timeout", queue);

        if (url == null) return;

        if (method == null) {
            method = (bodyTag != null) ? "POST" : "GET";
        }
        method = method.toUpperCase();

        long timeoutMillis = 10_000;
        if (timeoutTag != null) {
            try {
                timeoutMillis = new DurationTag(timeoutTag.identify()).getTicksLong() * 50L;
            } catch (Exception e) {
                Debugger.echoError(queue, "Invalid timeout format, defaulting to 10s.");
            }
        }

        HttpRequest.Builder requestBuilder;
        try {
            requestBuilder = HttpRequest.newBuilder().uri(URI.create(url.replace(" ", "%20")));
        } catch (IllegalArgumentException e) {
            Debugger.echoError(queue, "Invalid URL format: " + url + " | Did you forget to .urlEncode special characters?");
            return;
        }

        requestBuilder.timeout(Duration.ofMillis(timeoutMillis));

        String bodyString = null;
        String autoContentType = null;

        if (bodyTag != null) {
            if (payloadType == null) payloadType = "raw";

            switch (payloadType.toLowerCase()) {
                case "json":
                    bodyString = JsonHelper.toJson(bodyTag).toString();
                    autoContentType = "application/json; charset=utf-8";
                    break;

                case "form":
                case "formdata":
                    if (bodyTag instanceof MapTag map) {
                        StringBuilder form = new StringBuilder();
                        for (String key : map.keySet()) {
                            if (!form.isEmpty()) form.append("&");
                            form.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                            form.append("=");
                            form.append(URLEncoder.encode(map.getObject(key).identify(), StandardCharsets.UTF_8));
                        }
                        bodyString = form.toString();
                    } else {
                        Debugger.echoError(queue, "Fetch type 'form' requires the body to be a MapTag!");
                        return;
                    }
                    autoContentType = "application/x-www-form-urlencoded; charset=utf-8";
                    break;

                default:
                    bodyString = bodyTag.identify();
                    break;
            }
        }

        boolean hasUserContentType = false;
        if (headersTag instanceof MapTag mapTag) {
            for (String key : mapTag.keySet()) {
                requestBuilder.header(key, mapTag.getObject(key).identify());
                if (key.equalsIgnoreCase("Content-Type")) hasUserContentType = true;
            }
        }

        if (autoContentType != null && !hasUserContentType) {
            requestBuilder.header("Content-Type", autoContentType);
        }

        HttpRequest.BodyPublisher publisher = (bodyString == null)
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(bodyString, StandardCharsets.UTF_8);

        try {
            requestBuilder.method(method, publisher);
        } catch (IllegalArgumentException e) {
            Debugger.echoError(queue, "Invalid HTTP Method: " + method);
            return;
        }

        HttpResponse.BodyHandler<?> bodyHandler;
        if (saveFile != null) {
            try {
                File dataFolder = Corex.getInstance().getDataFolder();
                File targetFile = new File(dataFolder, saveFile);
                if (!targetFile.getCanonicalPath().startsWith(dataFolder.getCanonicalPath())) {
                    Debugger.echoError(queue, "Security error: savefile path must be within the Corex plugin folder!");
                    return;
                }
                targetFile.getParentFile().mkdirs();
                bodyHandler = HttpResponse.BodyHandlers.ofFile(targetFile.toPath());
            } catch (Exception e) {
                Debugger.echoError(queue, "Failed to resolve savefile path: " + e.getMessage());
                return;
            }
        } else {
            bodyHandler = HttpResponse.BodyHandlers.ofString();
        }

        Debugger.report(queue, instruction,
                "URL", url,
                "Method", method,
                "PayloadType", payloadType != null ? payloadType : "None",
                "IsWaitable", instruction.isWaitable
        );

        if (instruction.isWaitable) {
            queue.pause();
        }

        HTTP_CLIENT.sendAsync(requestBuilder.build(), bodyHandler).whenComplete((response, throwable) -> {
            SchedulerAdapter.get().runLater(() -> {
                MapTag result = new MapTag();

                if (throwable != null) {
                    result.putObject("failed", new ElementTag(true));
                    result.putObject("error", new ElementTag(throwable.getMessage()));
                    result.putObject("status", new ElementTag(0));
                } else {
                    int statusCode = response.statusCode();
                    result.putObject("failed", new ElementTag(statusCode < 200 || statusCode >= 400));
                    result.putObject("status", new ElementTag(statusCode));

                    if (saveFile == null) {
                        result.putObject("result", new ElementTag((String) response.body()));
                    }

                    MapTag responseHeaders = new MapTag();
                    for (Map.Entry<String, List<String>> h : response.headers().map().entrySet()) {
                        ListTag list = new ListTag();
                        h.getValue().forEach(list::addString);
                        responseHeaders.putObject(h.getKey() == null ? "http-version" : h.getKey(), list);
                    }
                    result.putObject("headers", responseHeaders);
                }

                CommandHelper.saveResult(queue, instruction, result);

                if (instruction.isWaitable) {
                    queue.resume();
                }
            }, 1L);
        });
    }
}