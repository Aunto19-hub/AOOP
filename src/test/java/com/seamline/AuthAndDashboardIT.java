package com.seamline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end check of the two screens this module covers: sign in, then read the
 * dashboard with the token that sign-in returned.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndDashboardIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LOGIN_BODY = """
            {"identifier":"farhana.akter@seamline.com","password":"seamline123"}
            """;

    @Test
    void validCredentialsReturnATokenAndProfile() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.fullName").value("Farhana Akter"))
                .andExpect(jsonPath("$.user.assignedLine").value("Line-07"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"farhana.akter@seamline.com\",\"password\":\"not-the-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankIdentifierFailsValidation() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"\",\"password\":\"seamline123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardReturnsKpisStationsAndCharts() throws Exception {
        String token = signIn();

        String body = mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line.code").value("Line-07"))
                .andExpect(jsonPath("$.style.name").value("Men's Polo Shirt"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("kpis").path("piecesThisShift").asInt()).isPositive();
        assertThat(json.path("stations").size()).isPositive();
        assertThat(json.path("hourly").size()).isEqualTo(8);
        assertThat(json.path("lossBreakdown").size()).isEqualTo(4);
        assertThat(json.path("alert").path("title").asText()).contains("constraining the line");
    }

    @Test
    void rerunAcceptsNewParameters() throws Exception {
        String token = signIn();

        mockMvc.perform(post("/api/dashboard/rerun")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algorithm\":\"greedy\",\"teamSize\":11,\"bufferPerStation\":1,\"seed\":777}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.algorithm").value("greedy"))
                .andExpect(jsonPath("$.run.teamSize").value(11))
                .andExpect(jsonPath("$.run.seed").value(777));
    }

    @Test
    void industrialEngineerScreensAreBackedByTheApi() throws Exception {
        String token = signIn();

        // Style breakdown
        mockMvc.perform(get("/api/styles/POLO-MEN").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationCount").value(16))
                .andExpect(jsonPath("$.operations[0].code").value("C01"));

        // Operators skill matrix
        mockMvc.perform(get("/api/operators").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(14))
                .andExpect(jsonPath("$[0].skills").isMap());

        // Line simulation
        mockMvc.perform(post("/api/simulation/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algorithm\":\"rpw\",\"teamSize\":14,\"bufferPerStation\":3,\"seed\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.stationCount").isNumber())
                // the engineer's own line (Line-07 = polo, 10.70 SMV), not whichever line sorts first
                .andExpect(jsonPath("$.plan.totalSmv").value(10.7))
                .andExpect(jsonPath("$.result.piecesProduced").isNumber());

        // Scenarios
        mockMvc.perform(get("/api/simulation/scenarios?seed=42").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarios.length()").value(5))
                .andExpect(jsonPath("$.scenarios[0].stationCount").value(org.hamcrest.Matchers.greaterThan(8)));
    }

    @Test
    void factoryScreensListLinesAndOrders() throws Exception {
        String token = signIn();

        mockMvc.perform(get("/api/lines").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].lineCode").isNotEmpty());

        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void theFrontEndIsServedByTheSameApp() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/app.html"))
                .andExpect(status().isOk());
    }

    private String signIn() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("accessToken").asText();
    }
}
