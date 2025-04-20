package com.dylibso.chicory.dwarf.rust;

public enum Language {
    UNKNOWN("Unknown"),
    C89("C89"),
    C("C"),
    Ada83("Ada83"),
    CPP("C++"),
    Cobol74("Cobol74"),
    Cobol85("Cobol85"),
    Fortran77("Fortran77"),
    Fortran90("Fortran90"),
    Pascal83("Pascal83"),
    Modula2("Modula2"),
    Java("Java"),
    C99("C99"),
    Ada95("Ada95"),
    Fortran95("Fortran95"),
    PLI("PLI"),
    ObjC("Objective-C"),
    ObjCPP("Objective-C++"),
    UPC("UPC"),
    D("D"),
    Python("Python"),
    OpenCL("OpenCL"),
    Go("Go"),
    Modula3("Modula3"),
    Haskell("Haskell"),
    CPP03("C++03"),
    CPP11("C++11"),
    OCaml("OCaml"),
    Rust("Rust"),
    C11("C11"),
    Swift("Swift"),
    Julia("Julia"),
    Dylan("Dylan"),
    CPP14("C++14"),
    Fortran03("Fortran03"),
    Fortran08("Fortran08"),
    RenderScript("RenderScript"),
    BLISS("BLISS"),
    MipsAssembler("Mips Assembler"),
    GoogleRenderScript("Google RenderScript"),
    SunAssembler("SUN Assembler"),
    AltiumAssembler("Altium Assembler"),
    BorlandDelphi("Borland Delphi");

    private final String value;

    Language(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Language fromName(String name) {
        for (Language lang : values()) {
            if (lang.value.equals(name)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unknown language: " + name);
    }

    @Override
    public String toString() {
        return value;
    }
}
