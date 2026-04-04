package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.utils.ConditionCompiler;
import org.jspecify.annotations.NonNull;

/* @[command]
 *
 * @Name If
 * @Syntax if [<value>] (!)(<operator> <value>) (&&/|| ...): [<commands>]
 * @RequiredArgs 1
 * @MaxArgs -1
 * @ShortDescription Compares values, and runs a subset of commands if they match.
 *
 * @Implements If
 *
 * @Description
 * Compares values, and runs a subset of commands if they match.
 * Works with the else command, which handles alternatives for when the comparison fails.
 * The If command is equivalent to the English phrasing "if something is true, then do the following".
 *
 * Values are compared using the comparable system. See {@link language operator} for information.
 *
 * Comparisons may be chained together using the symbols '&&' and '||' or their text equivalents 'and' and 'or'.
 * '&&' means "and", '||' means "or".
 * So, for example "if <[a]> && <[b]>:" requires both a AND b to be true.
 * "if <[a]> and <[b]>:" also requires both a AND b to be true.
 *
 * The "or" is inclusive, meaning "if <[a]> || <[b]>:" will pass for any of the following:
 * a = true, b = true
 * a = true, b = false
 * a = false, b = true
 * but will fail when a = false and b = false.
 *
 * Sets of comparisons may be grouped using ( parens ) as separate arguments.
 * So, for example "if ( <[a]> && <[b]> ) || <[c]>", or "if ( <[x]> or <[y]> or <[z]> ) and ( <[a]> or <[b]> or <[c]> )"
 * Grouping is REQUIRED when using both '&&' and '||' in one line. Otherwise, groupings should not be used at all.
 *
 * Boolean inputs and groups both support negating with the '!' symbol as a prefix.
 * This means you can do "if !<[a]>" to say "if a is NOT true".
 * Similarly, you can do "if !( <[a]> || <[b]> )", though be aware that per rules of boolean logic,
 * that example is the exactly same as "if !<[a]> && !<[b]>".
 *
 * You can also use keyword "not" as its own argument to negate a boolean or an operator.
 * For example, "if not <[a]>:" will require a to be false, and "if <[a]> not equals <[b]>:" will require that 'a' does not equal 'b'.
 *
 * When not using a specific comparison operator, true vs false will be determined by Truthiness, see {@link tag ObjectTag.is_truthy} for details.
 * For example, "- if <player||null>:" will pass if a player is linked, valid, and online.
 *
 * @Tags
 * <ObjectTag.is[<operator>].to[<element>]>
 * <ObjectTag.is[<operator>].than[<element>]>
 *
 * @Usage
 * // Use to narrate a message only if a player has a flag.
 * - if <player.has_flag[secrets]>:
 *     - narrate "The secret number is 3!"
 *
 * @Usage
 * // Use to narrate a different message depending on a player's money level.
 * - if <player.money> > 1000:
 *     - narrate "You're rich!"
 * - else:
 *     - narrate "You're poor!"
 *
 * @Usage
 * // Use to stop a script if a player doesn't have all the prerequisites.
 * - if !<player.has_flag[quest_complete]> || !<player.has_permission[new_quests]> || <player.money> < 50:
 *     - narrate "You're not ready!"
 *     - stop
 * - narrate "Okay so your quest is to find the needle item in the haystack build next to town."
 *
 * @Usage
 * // Use to perform a complicated requirements test before before changing some event.
 * - if ( poison|magic|melting contains <context.cause> and <context.damage> > 5 ) or <player.has_flag[weak]>:
 *     - determine <context.damage.mul[2]>
 */
public class IfCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "if";
    }

    @Override
    public @NonNull String getSyntax() { return "[<value>]"; }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return -1;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        ConditionCompiler.Condition condition = (ConditionCompiler.Condition) instruction.customData;
        if (condition == null) {
            condition = ConditionCompiler.compile(instruction.linearArgs);
            instruction.customData = condition;
        }

        boolean result = condition.evaluate(queue);

        queue.setTempData("corex_if_result", result);

        if (result && instruction.innerBlock != null) {
            queue.pushFrame(getName(), instruction.innerBlock, null);
        }
    }
}