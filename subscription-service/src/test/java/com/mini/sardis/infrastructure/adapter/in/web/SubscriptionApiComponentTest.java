package com.mini.sardis.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.BaseComponentTest;
import com.mini.sardis.domain.value.DiscountType;
import com.mini.sardis.infrastructure.adapter.in.web.dto.CreatePromoCodeRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubscriptionApiComponentTest extends BaseComponentTest {

    @Autowired ObjectMapper objectMapper;

    // Seeded by DataInitializer
    private static final String ADMIN_EMAIL    = "admin@demo.com";
    private static final String ADMIN_PASSWORD = "Admin1234";
    private static final String USER_EMAIL     = "user1@demo.com";
    private static final String USER_PASSWORD  = "Password1";

    // Plans seeded by V8 + V13 migrations
    private static final String PLAN_BASIC_1M  = "a0000001-0000-0000-0000-000000000001";
    private static final String PLAN_BASIC_12M = "a0000001-0000-0000-0000-000000000006";

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void login_withValidCredentials_returnsToken() throws Exception {
        String token = login(USER_EMAIL, USER_PASSWORD);
        Assertions.assertFalse(token.isBlank());
    }

    @Test
    @Order(2)
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + USER_EMAIL + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void register_newUser_returns201() throws Exception {
        String unique = "comp" + System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + unique + "@test.com\","
                        + "\"password\":\"Password1\","
                        + "\"fullName\":\"Component Tester\","
                        + "\"phoneNumber\":\"+905001111111\"}"))
                .andExpect(status().isCreated());
    }

    // ── Plans ────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    void getPlans_returnsAtLeastBasicPlans() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)));
    }

    @Test
    @Order(11)
    void getPlanById_returnsCorrectPlan() throws Exception {
        mockMvc.perform(get("/api/v1/plans/" + PLAN_BASIC_1M))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Basic"))
                .andExpect(jsonPath("$.durationDays").value(30));
    }

    @Test
    @Order(12)
    void get12MonthPlan_exists() throws Exception {
        mockMvc.perform(get("/api/v1/plans/" + PLAN_BASIC_12M))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationDays").value(365));
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    @Test
    @Order(20)
    void createSubscription_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":\"" + PLAN_BASIC_1M + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(21)
    void createSubscription_withCreditCard_returns202Pending() throws Exception {
        String token = login(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{\"planId\":\"" + PLAN_BASIC_1M + "\","
                        + "\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.finalAmount").isNumber());
    }

    @Test
    @Order(22)
    void createSubscription_withBankTransfer_returns202() throws Exception {
        String token = login(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{\"planId\":\"" + PLAN_BASIC_1M + "\","
                        + "\"paymentMethod\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(30)
    void getMySubscriptions_returnsUserSubscriptions() throws Exception {
        String token = login(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(get("/api/v1/subscriptions/my")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(31)
    void getAllSubscriptions_requiresAdminRole() throws Exception {
        String userToken = login(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(get("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(32)
    void getAllSubscriptions_asAdmin_returns200() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── Promo Codes ───────────────────────────────────────────────────────────

    @Test
    @Order(40)
    void createPromoCode_asAdmin_returns201() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "SAVE10PCT", DiscountType.PERCENTAGE, BigDecimal.valueOf(10),
                100, null, null, null);

        mockMvc.perform(post("/api/v1/admin/promo-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAVE10PCT"))
                .andExpect(jsonPath("$.discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.applicableMonths").isEmpty());
    }

    @Test
    @Order(41)
    void createPromoCode_with12MonthRestriction_persistsApplicableMonths() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "ANNUAL20", DiscountType.PERCENTAGE, BigDecimal.valueOf(20),
                50, null, null, Set.of(12));

        mockMvc.perform(post("/api/v1/admin/promo-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicableMonths").isArray())
                .andExpect(jsonPath("$.applicableMonths", contains(12)));
    }

    @Test
    @Order(42)
    void createPromoCode_asUser_returns403() throws Exception {
        String userToken = login(USER_EMAIL, USER_PASSWORD);
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "NOTALLWD", DiscountType.PERCENTAGE, BigDecimal.valueOf(5),
                null, null, null, null);

        mockMvc.perform(post("/api/v1/admin/promo-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(50)
    void validatePromoCode_nonExistent_returnsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/promo-codes/DOESNOTEXIST/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @Order(51)
    void validatePromoCode_existingCode_returnsValid() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        createPromoCode(adminToken, "TESTVALID", null);

        mockMvc.perform(get("/api/v1/promo-codes/TESTVALID/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.code").value("TESTVALID"));
    }

    @Test
    @Order(52)
    void validatePromoCode_with12MonthCode_invalidFor1Month() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        createPromoCodeWithMonths(adminToken, "ONLY12M", Set.of(12));

        mockMvc.perform(get("/api/v1/promo-codes/ONLY12M/validate?durationMonths=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @Order(53)
    void validatePromoCode_with12MonthCode_validFor12Months() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        createPromoCodeWithMonths(adminToken, "BEST12M", Set.of(12));

        mockMvc.perform(get("/api/v1/promo-codes/BEST12M/validate?durationMonths=12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @Order(60)
    void createSubscription_withApplicablePromo_appliesDiscount() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String userToken  = login(USER_EMAIL, USER_PASSWORD);
        createPromoCode(adminToken, "DISC15", null);

        mockMvc.perform(post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .content("{\"planId\":\"" + PLAN_BASIC_1M + "\","
                        + "\"promoCode\":\"DISC15\","
                        + "\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.discountAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.finalAmount").value(lessThan(49.99)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private void createPromoCode(String adminToken, String code, Set<Integer> months) throws Exception {
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                code, DiscountType.PERCENTAGE, BigDecimal.valueOf(15),
                null, null, null, months);
        mockMvc.perform(post("/api/v1/admin/promo-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private void createPromoCodeWithMonths(String adminToken, String code, Set<Integer> months) throws Exception {
        createPromoCode(adminToken, code, months);
    }
}
