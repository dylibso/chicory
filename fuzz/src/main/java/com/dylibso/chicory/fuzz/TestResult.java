package com.dylibso.chicory.fuzz;

public class TestResult {

    private String oracleResult;
    private String chicoryResult;

    public TestResult(String oracleResult, String chicoryResult) {
        this.oracleResult = oracleResult;
        this.chicoryResult = chicoryResult;
    }

    public String getOracleResult() {
        return oracleResult;
    }

    public String getChicoryResult() {
        return chicoryResult;
    }
}
