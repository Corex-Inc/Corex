package dev.corexinc.corex.engine.compiler.math;

import dev.corexinc.corex.engine.queue.ScriptQueue;

@FunctionalInterface
public interface MathNode {
    double eval(ScriptQueue queue);
}