---
slug: first-announcement
title: A zero dependency Wasm runtime for the JVM
authors: [andreaTP, bhelx]
tags: [wasm, chicory]
---

# A zero dependency Wasm runtime for the JVM

## All I want for Xmas is Chicory

<img src="/img/blog-2023-12-25/main.png"/>
<!-- truncate -->

## A zero dependency Wasm runtime for the JVM

## INTRO

In a lot of cultures, during Christmas time, Santa would distribute toys and presents to the good kids around. If you are reading Java Advent Of Code, that means that you care about Java and I’m sure you are a good one who deserves a present\!

Let me be your Santa today and I’ll show you a new shiny present:

From the outside, the box looks small. We are not going to find any full-fledged framework to write Enterprise applications.

On the front, there are pictures of mythical integrations and fancy plugin systems, but a note on the back catches the attention: “requires assembly”. Gotcha, this is the kind of educational toy that requires us to read the guide to make some sense out of the pieces.

Are you ready for the unboxing?  
Here you have a fancy early version of a Wasm interpreter called Chicory\!

Whaaaat????  
What does that even mean?  
Don’t get upset, the instructions are detailed, let’s go through them together.

## WebAssembly (Wasm)

From Wiki we can read:

> defines a portable binary-code format and a corresponding text format for executable programs as well as software interfaces for facilitating interactions between such programs and their host environment.

[https://en.wikipedia.org/wiki/WebAssembly](https://en.wikipedia.org/wiki/WebAssembly)

Ok, I get it, it’s another bytecode format to express programs, but how and why should we use it? Is the JVM bytecode not good enough?

Wasm has some different characteristics that give it an advantage over the JVM for some use cases. Because it was born on the web, Wasm has a sandboxed memory model which prevents modules from reading memory or jumping to code outside of their own scope. And by default, Wasm will not allow you to access any system resources such as files or networks. Wasm also has no concepts of objects or heap and that means it can run low-level languages very efficiently. This makes Wasm an ideal candidate for running untrusted / third party code written in a variety of languages.

## Included pieces

Wasm has something like an assembly language that maps directly to its instructions. You can write this by hand, but like Assembly, you typically don't. Fortunately, we do have much better and higher level languages to solve the nitty-gritty problems of the low-level for us.

Many of the most popular languages are starting to offer the possibility of TARGETING WASM, translating your program written in a high-level programming language into a series of Wasm instructions.

<img src="/img/blog-2023-12-25/image1.png"/>

Often, the instructions for educational games are informative, but you don’t truly understand it until you play it yourself. Let’s connect the first pieces and compile a simple program to Wasm.

The program:

<img src="/img/blog-2023-12-25/image2.png"/>

Compile with:

```bash
rustc count\_vowels.rs \--target=wasm32-unknown-unknown \--crate-type=cdylib \-C opt-level=0 \-C debuginfo=0 \-o count\_vowels.wasm
```

For simplicity, you can use the provided [Dockerfile](https://github.com/andreaTP/first-chicory-blog/blob/main/compile_to_wasm/Dockerfile).

Now that we have a \`.wasm\` file to try things out we need “something” to run the instructions.

Without getting too fancy and cutting nuances off we have two main options:

- Another compiler: This should be able to take the \`wasm\` format and translate it to machine code that a computer can run (e.g. [wasm2c](https://github.com/WebAssembly/wabt/tree/main/wasm2c))  
- An interpreter: This will process the content of the \`wasm\` file directly executing the instructions.

Chicory, as of today, is a Pure Java (no dependencies other than the Java standard library) interpreter and there are tradeoffs to consider here:

- Compiler:  
  - **PRO**: fast execution  
  - **CONS**: needs a “developer” toolchain to be executed, more opportunity for security vulnerabilities  
- Interpreter:  
  - **PRO**: self-contained and easily portable, less opportunity for security vulnerabilities  
  - **CONS**: can be slow for heavy compute programs

More specifically a compiler can apply optimizations and transformations before emitting the target output, effectively making the resulting binary, usually, better in terms of speed and efficiency.  
An interpreter instead can directly run WASM code without the need for additional toolchains, this makes it a natural fit for running arbitrary, user-defined functions unknown at compile time of your application.

Chicory is doing a direct mapping from the Wasm format to the JVM's native primitives, which doesn’t need any advanced technique like introspection and, more importantly, reflection making it a great candidate for embedding in [GraalVM `native-image` binaries](https://www.graalvm.org/22.0/reference-manual/native-image/).

Using our experience and test suite of the interpreter engine, we're also working on a compiler from Wasm to JVM Bytecode. As demonstrated in the Go ecosystem with Wazero both an interpreter and a compiler are extremely useful for different use cases.

Now, let’s stop looking at the separately sold additional pieces of our present and keep reading the instructions :-)

## Usage instructions

When we start thinking about the possible use cases for a WASM interpreter, the sky's the limit. But, let’s look closer at the examples to see how we can employ such technology.

- **Enable polyglot plugin systems**  
  [Kroxylicious](https://kroxylicious.io/) is a pluggable proxy for Kafka, it enables you to write “filters” in Java to perform various kinds of operations, for example, data manipulation of the messages.  
  [In this example](https://github.com/kroxylicious/kroxylicious/pull/721) you can see how to plug in the Chicory interpreter and automagically offer support for plugins written in other languages. [Extism](https://extism.org/) is another great example of an ultra-portable plug-in system.   
    
- **Reuse of libraries from different ecosystems**  
  It’s not always desirable to rewrite a functionality in multiple different languages, for the sake of speed, correctness and maintenance.  
    
- **Dynamic functions execution**  
  [GraalVM native-image](https://www.graalvm.org/22.0/reference-manual/native-image/) makes it viable for Java programs to be compiled into static binaries. The downside is that the usage of reflection becomes an obstacle and should be configured Ahead Of Time. This takes out a good slice of “dynamicity”.  
  [In this example](https://github.com/andreaTP/first-chicory-blog/tree/main/dynamic-loading) we are including Chicory in a statically built CLI binary that can execute user-provided code provided via the command line.

So, the pictures are nice, now let’s try to write and run an example on our own.

<img src="/img/blog-2023-12-25/image3.png"/>

Now we are going to write a Quarkus Web Server from scratch that will expose two endpoints:

- Load code dynamically  
- Execute the loaded code on the user input


  
You can think of it like a mini Java-powered "functions as a service" platform.  And with native image, we will be compiling it to native binaries so it should be fast.

## Step-by-step

1. [Install the Quarkus CLI](https://quarkus.io/get-started/) and create a standard Quarkus application:  
     
   `quarkus create`  
     
   This command will automatically scaffold a full-blown server-side application in the folder `code-with-quarkus` , so open it with your favorite IDE/Editor and we can start to play\!

2. Fix the dependencies in `pom.xml` as shown in this diff:
    <img src="/img/blog-2023-12-25/image4.png"/>
    - use plain RestEasy (as opposed to the default reactive version)  
    - add the dependency on Jackson to handle Json objects from your API  
    - add the dependency on the Chicory runtime\! ( as described in the project [Readme](https://github.com/dylibso/chicory#install-dependency) )


3. Remove the default content in `src/main/java/org/acme` and scaffold your implementation with the content available [here](https://github.com/andreaTP/first-chicory-blog/tree/main/quarkus/scaffold).  
   In the code we are defining a stateful “Service” shared between 2 “Resource”s depending on it:  
    - “WasmResource” defines one endpoint able to receive a wasm file in binary format  
    - “ComputeResource” is the endpoint that will provide the final “functionality” executing the last wasm function uploaded against the user input  
    - the “WasmService” example API exposes two methods to perform the desired operations:

    <img src="/img/blog-2023-12-25/image5.png"/>


4. Fill the empty “WasmService” implementation to finally perform the desired actions using Chicory’s primitives

    <img src="/img/blog-2023-12-25/image6.png"/>
	

    - “setModule” takes an `InputStream` and builds a Wasm Module out of it, reading the structure and the instructions contained. The Module will be stored into an example local variable to be reused.  
    - “compute” is a method that takes a simple user input (an `int` \!) and perform the rest of the operations:  
        - instantiate the module to be ready to run  
        - find a declared function named “exported\_function”  
        - invoke the function passing the user content to it  
        - return the result as an “int”

5. Congratulations\! You have just written your first application server leveraging a Wasm interpreter\! Let’s test it out by running the server in a shell:  
   `mvn quarkus:dev`  
   Spin another shell and use the endpoints:  
    - let’s first upload a Wasm module, to try things out in an easy way you can use the pre-built example (feel free to modify and recompile it\!):

        ```
        curl \-sL https://raw.githubusercontent.com/andreaTP/first-chicory-blog/main/dynamic-loading/example.wasm  \--output example.wasm
        ```

        and upload it to the running web-server:

        ```
        curl \-H 'Content-Type: application/octet-stream' \-X POST \--data-binary @example.wasm http://localhost:8080/wasm
        ```

    - finally, we can call the “compute” endpoint:

        ```
        curl \-v 'http://localhost:8080/compute' \-H 'Content-Type: application/json' \--data-raw '41'
        ```

    - and we should receive the long-awaited answer:

        ```
        {"value":42}
        ```

        as the compiled example [is simply adding 1 to the user input](https://github.com/andreaTP/first-chicory-blog/blob/5a0fe4f8b22809d7b0d932348ec4f826714d30b6/dynamic-loading/example.rs#L3).

6. To make sure that there is nothing that depends on the development environment we can now containerize our application. Drop a file named `Dockerfile.native-scratch` in the `src/main/docker` folder of our Quarkus application, and fill it with the following content (adapted from the [official documentation](https://quarkus.io/guides/building-native-image#build-a-container-image-from-scratch)):  
     
```docker
FROM quay.io/quarkus/ubi-quarkus-graalvmce-builder-image:jdk-21 AS build  
USER root  
RUN microdnf install make gcc  
RUN mkdir /musl && \\  
    curl \-L \-o musl.tar.gz https://more.musl.cc/11.2.1/x86\_64-linux-musl/x86\_64-linux-musl-native.tgz && \\  
    tar \-xvzf musl.tar.gz \-C /musl \--strip-components 1 && \\  
    curl \-L \-o zlib.tar.gz https://www.zlib.net/zlib-1.3.tar.gz && \\  
    mkdir zlib && tar \-xvzf zlib.tar.gz \-C zlib \--strip-components 1 && \\  
    cd zlib && ./configure \--static \--prefix=/musl && \\  
    make && make install && \\  
    cd .. && rm \-rf zlib && rm \-f zlib.tar.gz && rm \-f musl.tar.gz  
ENV PATH\="/musl/bin:${PATH}"  
USER quarkus  
WORKDIR /code  
COPY . .  
RUN ./mvnw package \-Dnative \-DskipTests \-Dquarkus.native.additional-build-args="--static","--libc=musl"  
    
*\#\# Stage 2 : create the final image*  
FROM scratch  
COPY \--from\=build /code/target/\*-runner /application  
EXPOSE 8080  
ENTRYPOINT \[ "/application" \]  
```   

	Build the container image:
    ```
	docker build -f src/main/docker/Dockerfile.native-scratch -t chicory/getting-started .
    ```

	And run it:
    ```
    docker run -i --rm -p 8080:8080 chicory/getting-started
    ```
	  
You can notice that the final image is built `FROM scratch` making sure that our application will not be accessing any system-level resource. You can exercise the built image by using the same `curl` demo commands provided before.

Pretty cool, right?  
Now you have a toy server that can run your users' Wasm code against the user input running as a *native-image* \!

You can find all the code we used in this exercise [here](https://github.com/andreaTP/first-chicory-blog/tree/main/quarkus).

## Warranty

Chicory is in an early stage of development, there are rough edges and the current implementation doesn’t cover the full specification just yet. Although we're missing some pieces of the puzzle, now is a great time to join and help\!

If you are brave enough to try things out, please report any failure along with your use case\! We are eager to receive early feedback on the current work.

## Next

Thanks for bearing with these instructions, I hope that you are reading this final comment while basking, feeling accomplished, with a working example.

We can silently leave the room, to get back, heads down, implementing the last details of the WebAssembly specification.

This year we have built a little rocket toy, but we strongly believe that this innovation brings great benefits to Java and the ecosystem and we are looking at landing on the Moon and making this project a solid building block for the Java applications of tomorrow\!

### What else?

### Demo repo:

[​​https://github.com/andreaTP/first-chicory-blog](https://github.com/andreaTP/first-chicory-blog)

### Acknowledgment/Further reads:

Edoardo Vacchi has written a series of 2 blog posts to track the progress of Wasm related projects especially targeting Java developers:

- [https://www.javaadvent.com/2022/12/webassembly-for-the-java-geek.html](https://www.javaadvent.com/2022/12/webassembly-for-the-java-geek.html)  
- [https://www.javaadvent.com/2023/12/a-return-to-webassembly-for-the-java-geek.html](https://www.javaadvent.com/2023/12/a-return-to-webassembly-for-the-java-geek.html)


### Similar projects:

[GraalVM implementation of WebAssembly](https://www.graalvm.org/latest/reference-manual/wasm/) objectives are pretty much the same as Chicory’s, and this implementation is, at the time of writing, somehow more mature and complete.  
Chicory’s differentiates itself for a few, but we believe compelling, reasons:

- Zero dependencies: Chicory’s doesn’t need any additional dependency(other than itself obviously)  
- No platform lock-in: Chicory’s can, and will always, run on any compatible implementation of the JVM as it’s not based on any opinionated framework.

## Bio:

### Andrea Peruffo

Andrea Peruffo is an all-around software developer with experience in delivering software systems of any kind. He has experience in many different fields, like embedded controllers, cloud infrastructures, PLCs, BigData platforms, FPGAs, microservices etc. etc. Fond in hands-on code development and contributor to several Open Source projects.

### Benjamin Eckel

Benjamin Eckel is the CTO and co-founder of Dylibso, a company focused on accelerating Wasm adoption and tooling.
