---
slug: syntax-highlight
title: 'Syntax highlighting in Java, without the pain'
authors: [andreaTP]
tags: [wasm, chicory, highlight, tree-sitter, highlight.js]
---

<!-- truncate -->

# Syntax Highlighting in Java Without Writing a Java Library

A week ago, my friend Max told me he couldn't find a good library for code syntax highlighting in Java.

That statement alone says a lot.

Syntax highlighting is a solved problem. It’s not novel, it’s not research-grade, and yet in the Java ecosystem it still feels complex, expensive, and tedious to integrate.
You either end up with half-maintained ports, JNI bindings, or large dependencies that are painful to distribute and maintain.

The real question is:
**how do we make things that were previously considered complex, expensive, and fragile easy, robust, and boring?**

## The traditional options (and their cost)

Sure, with the help of LLMs, it's now much easier than before to port a library from another language to Java.
But that’s only the first step.
Once you port it, you **own it**:

* ongoing maintenance
* vulnerability fixes
* bug reports
* releases
* code drift with the upstream

That maintenance burden never goes away.

Fortunately, in many cases, the library we want already exists and is excellent, just not written in Java.

## Wasm to the rescue

WebAssembly changes the equation.

With Wasm, we can reuse libraries written in **C, C++, Go, Rust, or even JavaScript**, without falling into rewriting or the usual JNI traps:

* no platform-specific builds
* no cross-compilation matrix
* no native binaries to ship
* no “works on my machine” surprises

Everything runs in a sandboxed, lightweight, and portable runtime.

## Two candidates: highlight.js and tree-sitter

When Max asked about highlighting, two obvious candidates came to mind:

* **highlight.js**
* **tree-sitter**

### highlight.js via QuickJS

For `highlight.js`, the fastest proof of concept was to run it inside a JavaScript engine.

Using **QuickJs4J**
[https://github.com/roastedroot/quickjs4j](https://github.com/roastedroot/quickjs4j)

I put together a quick experiment:
[https://github.com/andreaTP/highlightjs4j](https://github.com/andreaTP/highlightjs4j)

QuickJs4J exists to embed the QuickJS engine into the JVM.
As long as a JS library doesn’t depend on browser or Node APIs, QuickJS is a very capable engine and can run surprisingly complex code with ease.

The experiment worked well and validated the approach.

### tree-sitter and native performance

Tree-sitter is even more appealing:

* written in C
* widely used in editors and IDEs
* extremely fast
* grammar-based and precise

Looking around, there are several projects in this space, among others:

* [arborium](https://github.com/bearcove/arborium)
* [syntastica](https://github.com/RubixDev/syntastica)
* [lumis](https://github.com/leandrocp/lumis)

In the end, **Lumis** stood out:
[https://github.com/leandrocp/lumis](https://github.com/leandrocp/lumis)

## Why Lumis on Wasm works so well

Lumis is written in Rust, which already has first-class support for compiling to WebAssembly.
Even better, it works out of the box with **WASI**.

That means:

* printing to stdout just works
* debugging is straightforward
* no weird shims or hacks

This made it a perfect candidate.

## Lumis4J: wrapping once, reusing forever

That’s how **Lumis4J** started:
[https://github.com/roastedroot/lumis4j](https://github.com/roastedroot/lumis4j)

The idea is simple:

* compile Lumis to Wasm and then to Java Bytecode thanks to Chicory
* write a small Rust wrapper exposing a clean API
* map that API to Java

The [Rust side](https://github.com/roastedroot/lumis4j/blob/main/wasm-build/src/lib.rs) is intentionally minimal.
It exposes just enough surface to drive Lumis without leaking implementation details.
On the Java side, this maps to a small, idiomatic API that feels native to the JVM:

```java
try (Lumis lumis = Lumis.builder()
                        .withLang(Lang.JAVA)
                        .withTheme(Theme.CATPPUCCIN_FRAPPE)
                        .withFormatter(Formatter.HTML_INLINE)
                        .build()) {
    var htmlResult = lumis.highlight("public class Hello { }");
    System.out.println(htmlResult.string());
}
```

Result:
<pre class="athl" style="color: #c6d0f5; background-color: #303446;"><code class="language-java" translate="no" tabindex="0"><div class="line" data-line="1"><span style="color: #ca9ee6;">public</span> <span style="color: #ca9ee6;">class</span> <span style="color: #e5c890;">Hello</span> <span style="color: #949cbb;">&lbrace;</span> <span style="color: #949cbb;">&rbrace;</span>
</div></code></pre>

You can easily run a minimal JBang [demo](https://gist.github.com/andreaTP/6945e9ff3223686b600fda184ae94e2f):

```bash
jbang https://gist.github.com/andreaTP/6945e9ff3223686b600fda184ae94e2f
```

The core of the integration is tiny. What would have been a massive port a few years ago is now just glue code.

Full reuse, zero reimplementation.

## Performance: Wizer to the rescue

Out of the box, Lumis works great. One issue remained: loading a language the first time could take seconds.

Enter **Wizer**.
[https://github.com/bytecodealliance/wizer](https://github.com/bytecodealliance/wizer)

Wizer is a Wasm pre-initializer.
It allows you to pre-load data and state at build time, so the module is already warm at runtime.
By preloading grammars and themes, startup time drops dramatically and, even the first invocation, is finally fast.

## Shipping the result

The final library includes:

* all languages
* all themes
* fully battery-included

Numbers:

* around 80Mb of Wasm payload
* compiled to Java bytecode via **Chicory**
* less than 10Mb final jar

And the resulting artifact is:

* easy to distribute
* no native dependencies
* runs on the JVM and Android
* fully self-contained
* minimal Java dependencies, just the Chicory runtime

But most importantly, it is **secure**.

The Wasm sandbox uses a heap fully separated from the JVM heap.
Any memory bugs or vulnerabilities at the C or Rust level surface as normal Java exceptions, not undefined behavior, not crashes, not security nightmares.

## The bigger picture

WebAssembly is a W3C standard. More and more libraries are quietly adding Wasm support to run in browsers, and that same support can be reused outside the browser, including on the JVM.

We should expect this trend to steadily continue.

## Conclusion

Being able to reuse complex, high-quality libraries everywhere, while staying within JVM boundaries, feels like a superpower.

Wasm unlocks scenarios that used to be impractical or outright impossible in Java, and it does so with less code, less risk, and less maintenance.

Thanks for reading. I hope it’s clear by now, syntax highlighting was just the excuse ✨
