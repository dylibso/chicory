import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OpaTest {

    @Test
    public void opaModule() {
        // Arrange
        var opa = new OpaTestModule();

        // Act
        var ctxAddr = opa.opaEvalCtxNew();
        var input = "{\"user\": \"alice\"}";
        var inputStrAddr = opa.opaMalloc(input.length());
        opa.instance().memory().writeCString(inputStrAddr, input);
        var inputAddr = opa.opaJsonParse(inputStrAddr, input.length());
        opa.opaFree(inputStrAddr);
        opa.opaEvalCtxSetInput(ctxAddr, inputAddr);

        var data = "{ \"role\" : { \"alice\" : \"admin\", \"bob\" : \"user\" } }";
        var dataStrAddr = opa.opaMalloc(data.length());
        opa.instance().memory().writeCString(dataStrAddr, data);
        var dataAddr = opa.opaJsonParse(dataStrAddr, data.length());
        opa.opaFree(dataStrAddr);
        opa.opaEvalCtxSetData(ctxAddr, dataAddr);

        var evalResult = opa.eval(ctxAddr);

        int resultAddr = opa.opaEvalCtxGetResult(ctxAddr);
        int resultStrAddr = opa.opaJsonDump(resultAddr);
        var resultStr = opa.instance().memory().readCString(resultStrAddr);
        opa.opaFree(resultStrAddr);

        // Assert
        assertEquals(0, evalResult);
        assertEquals("[{\"result\":true}]", resultStr);
    }
}
