package com.mini.sardis.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.sardis.BaseComponentTest;
import com.mini.sardis.domain.value.DiscountType;
import com.mini.sardis.infrastructure.adapter.in.web.dto.CreatePromoCodeRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubscriptionApiComponentTest extends BaseComponentTest {

    @Autowired ObjectMapper objectMapper;

    private static final String ADMIN_EMAIL    = "admin@demo.com";
    private static final String ADMIN_PASSWORD = "Admin1234";
    private static final String USER_EMAIL     = "user1@demo.com";
    private static final String USER_PASSWORD  = "Password1";

    private static final String PLAN_BASIC_1M  = "a0000001-0000-0000-0000-000000000001";
    private static final String PLAN_BASIC_12M = "a0000001-0000-0000-0000-000000000006";

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void login_withValidCredentials_returnsToken() {
        String token = login(USER_EMAIL, USER_PASSWORD);
        assertThat(token).isNotBlank();
    }

    @Test
    @Order(2)
    void login_withWrongPassword_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"email\":\"" + USER_EMAIL + "\",\"password\":\"wrong\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/login", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(3)
    void register_newUser_returns201() {
        String unique = "comp" + System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + unique + "@test.com\","
                + "\"password\":\"Password1\","
                + "\"fullName\":\"Component Tester\","
                + "\"phoneNumber\":\"+905001111111\"}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ── Plans ────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    void getPlans_returnsAtLeastBasicPlans() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/plans", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @Order(11)
    void getPlanById_returnsCorrectPlan() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/plans/" + PLAN_BASIC_1M, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("name").asText()).isEqualTo("Basic");
        assertThat(body.get("durationDays").asInt()).isEqualTo(30);
    }

    @Test
    @Order(12)
    void get12MonthPlan_exists() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/plans/" + PLAN_BASIC_12M, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("durationDays").asInt()).isEqualTo(365);
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    @Test
    @Order(20)
    void createSubscription_withoutAuth_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"planId\":\"" + PLAN_BASIC_1M + "\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(21)
    void createSubscription_withCreditCard_returns202Pending() throws Exception {
        String token = login(USER_EMAIL, USER_PASSWORD);
        HttpEntity<String> request = jsonRequestWithAuth(
                "{\"planId\":\"" + PLAN_BASIC_1M + "\",\"paymentMethod\":\"CREDIT_CARD\"}", token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asText()).isEqualTo("PENDING");
        assertThat(body.get("finalAmount").isNumber()).isTrue();
    }

    @Test
    @Order(22)
    void createSubscription_withBankTransfer_returns202() throws Exception {
        String token = login(USER_EMAIL, USER_PASSWORD);
        HttpEntity<String> request = jsonRequestWithAuth(
                "{\"planId\":\"" + PLAN_BASIC_1M + "\",\"paymentMethod\":\"BANK_TRANSFER\"}", token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    @Order(30)
    void getMySubscriptions_returnsUserSubscriptions() throws Exception {
        String token = login(USER_EMAIL, USER_PASSWORD);
        HttpEntity<Void> request = authOnlyRequest(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/subscriptions/my", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
    }

    @Test
    @Order(31)
    void getAllSubscriptions_requiresAdminRole() {
        String userToken = login(USER_EMAIL, USER_PASSWORD);
        HttpEntity<Void> request = authOnlyRequest(userToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(32)
    void getAllSubscriptions_asAdmin_returns200() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        HttpEntity<Void> request = authOnlyRequest(adminToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
    }

    // ── Promo Codes ───────────────────────────────────────────────────────────

    @Test
    @Order(40)
    void createPromoCode_asAdmin_returns201() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "SAVE10PCT", DiscountType.PERCENTAGE, BigDecimal.valueOf(10),
                100, null, null, null);
        HttpEntity<String> request = jsonRequestWithAuth(objectMapper.writeValueAsString(req), adminToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/promo-codes", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("SAVE10PCT");
        assertThat(body.get("discountType").asText()).isEqualTo("PERCENTAGE");
        assertThat(body.get("applicableMonths").isNull() || body.get("applicableMonths").isEmpty()).isTrue();
    }

    @Test
    @Order(41)
    void createPromoCode_with12MonthRestriction_persistsApplicableMonths() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "ANNUAL20", DiscountType.PERCENTAGE, BigDecimal.valueOf(20),
                50, null, null, Set.of(12));
        HttpEntity<String> request = jsonRequestWithAuth(objectMapper.writeValueAsString(req), adminToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/promo-codes", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("applicableMonths").isArray()).isTrue();
        assertThat(body.get("applicableMonths").get(0).asInt()).isEqualTo(12);
    }

    @Test
    @Order(42)
    void createPromoCode_asUser_returns403() throws Exception {
        String userToken = login(USER_EMAIL, USER_PASSWORD);
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "NOTALLWD", DiscountType.PERCENTAGE, BigDecimal.valueOf(5),
                null, null, null, null);
        HttpEntity<String> request = jsonRequestWithAuth(objectMapper.writeValueAsString(req), userToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/promo-codes", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(50)
    void validatePromoCode_nonExistent_returnsInvalid() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/promo-codes/DOESNOTEXIST/validate", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("valid").asBoolean()).isFalse();
    }

    @Test
    @Order(51)
    void validatePromoCode_existingCode_returnsValid() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        createPromoCode(adminToken, "TESTVALID", null);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/promo-codes/TESTVALID/validate", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("valid").asBoolean()).isTrue();
        assertThat(body.get("code").asText()).isEqualTo("TESTVALID");
    }

    @Test
    @Order(52)
    void validatePromoCode_with12MonthCode_invalidFor1Month() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        createPromoCode(adminToken, "ONLY12M", Set.of(12));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/promo-codes/ONLY12M/validate?durationMonths=1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("valid").asBoolean()).isFalse();
    }

    @Test
    @Order(53)
    void validatePromoCode_with12MonthCode_validFor12Months() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        createPromoCode(adminToken, "BEST12M", Set.of(12));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/promo-codes/BEST12M/validate?durationMonths=12", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("valid").asBoolean()).isTrue();
    }

    @Test
    @Order(60)
    void createSubscription_withApplicablePromo_appliesDiscount() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String userToken  = login(USER_EMAIL, USER_PASSWORD);
        createPromoCode(adminToken, "DISC15", null);

        HttpEntity<String> request = jsonRequestWithAuth(
                "{\"planId\":\"" + PLAN_BASIC_1M + "\","
                        + "\"promoCode\":\"DISC15\","
                        + "\"paymentMethod\":\"CREDIT_CARD\"}", userToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/subscriptions", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("discountAmount").decimalValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(body.get("finalAmount").decimalValue()).isLessThan(new BigDecimal("49.99"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/login", HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(response.getBody()).get("token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse login token", e);
        }
    }

    private HttpEntity<String> jsonRequestWithAuth(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authOnlyRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private void createPromoCode(String adminToken, String code, Set<Integer> months) throws Exception {
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                code, DiscountType.PERCENTAGE, BigDecimal.valueOf(15),
                null, null, null, months);
        HttpEntity<String> request = jsonRequestWithAuth(objectMapper.writeValueAsString(req), adminToken);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/promo-codes", HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
