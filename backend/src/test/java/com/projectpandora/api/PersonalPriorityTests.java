package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:pandora_priorities;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PersonalPriorityTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    static final String URL = "/api/v1/priorities/personal";

    String login(String name) throws Exception {
        var response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", name, "password", "demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse();
        return "Bearer " + json.readTree(response.getContentAsString(StandardCharsets.UTF_8)).get("token").asText();
    }
    ResultActions save(String token, long revision, Object... items) throws Exception {
        return mvc.perform(put(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("revision", revision, "items", List.of(items)))));
    }
    JsonNode body(ResultActions request) throws Exception {
        return json.readTree(request.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
    JsonNode read(String token) throws Exception { return body(mvc.perform(get(URL).header("Authorization", token))); }

    @Test void startsEmptyAndRoundTripsTrimmedItemsIntoPanel() throws Exception {
        String token = login("staff");
        assertThat(read(token).get("revision").asLong()).isZero();
        assertThat(read(token).get("items").size()).isZero();
        var saved = body(save(token, 0, Map.of("id", -42, "title", "  First  ", "detail", "Full detail", "sortOrder", 99), Map.of("title", "Second")));
        assertThat(saved.get("revision").asLong()).isEqualTo(1);
        var items = saved.get("items");
        assertThat(items.get(0).get("id").asLong()).isPositive();
        assertThat(items.get(1).get("id").asLong()).isNotEqualTo(items.get(0).get("id").asLong());
        assertThat(items.get(0).get("title").asText()).isEqualTo("First");
        assertThat(items.get(0).get("sortOrder").asInt()).isZero();
        assertThat(items.get(1).get("sortOrder").asInt()).isEqualTo(1);
        assertThat(items.get(0).get("myRemark").isNull()).isTrue();
        assertThat(read(token)).isEqualTo(saved);
        mvc.perform(get("/api/v1/panels/map").header("Authorization", token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.personalTop[0].title").value("First"))
            .andExpect(jsonPath("$.personalTop[0].detail").value("Full detail"));
    }
    @Test void replacementUpdatesReordersAndDeletesWithoutChangingRetainedIds() throws Exception {
        String token = login("staff");
        var items = body(save(token, 0, Map.of("title", "A"), Map.of("title", "B"), Map.of("title", "C"))).get("items");
        long a = items.get(0).get("id").asLong(), c = items.get(2).get("id").asLong();
        var updated = body(save(token, 1, Map.of("id", c, "title", "C changed", "detail", "new"), Map.of("id", a, "title", "A")));
        assertThat(updated.get("revision").asLong()).isEqualTo(2);
        assertThat(updated.get("items").size()).isEqualTo(2);
        assertThat(updated.at("/items/0/id").asLong()).isEqualTo(c);
        assertThat(updated.at("/items/0/title").asText()).isEqualTo("C changed");
        assertThat(updated.at("/items/1/id").asLong()).isEqualTo(a);
        assertThat(read(token)).isEqualTo(updated);
        var cleared = body(save(token, 2));
        assertThat(cleared.get("revision").asLong()).isEqualTo(3);
        assertThat(read(token).get("items").size()).isZero();
    }
    @Test void anotherUsersIdsCannotBeReadOrAdopted() throws Exception {
        String staff = login("staff"), other = login("founder");
        long id = body(save(staff, 0, Map.of("title", "Private"))).at("/items/0/id").asLong();
        assertThat(read(other).get("items").size()).isZero();
        save(other, 0, Map.of("id", id, "title", "Hijacked")).andExpect(status().isBadRequest());
        assertThat(read(staff).at("/items/0/title").asText()).isEqualTo("Private");
        assertThat(read(other).get("revision").asLong()).isZero();
    }
    @Test void rejectsMissingAndDuplicateExistingIdsAtomically() throws Exception {
        String token = login("staff");
        long id = body(save(token, 0, Map.of("title", "Original"))).at("/items/0/id").asLong();
        save(token, 1, Map.of("id", id, "title", "Changed"), Map.of("id", id, "title", "Duplicate")).andExpect(status().isBadRequest());
        save(token, 1, Map.of("id", Long.MAX_VALUE, "title", "Unknown")).andExpect(status().isBadRequest());
        assertThat(read(token).at("/items/0/title").asText()).isEqualTo("Original");
        assertThat(read(token).get("revision").asLong()).isEqualTo(1);
    }
    @Test void staleRevisionCannotOverwriteSavedItems() throws Exception {
        String token = login("staff");
        body(save(token, 0, Map.of("title", "Original")));
        save(token, 0, Map.of("title", "Stale edit")).andExpect(status().isConflict());
        assertThat(read(token).at("/items/0/title").asText()).isEqualTo("Original");
    }
    @Test void validatesRequiredRevisionListAndItemFields() throws Exception {
        String token = login("staff");
        for (String invalid : List.of("{}", "{\"items\":[]}", "{\"revision\":0}", "{\"revision\":-1,\"items\":[]}",
            "{\"revision\":0,\"items\":[null]}", "{\"revision\":0,\"items\":[{}]}")) {
            mvc.perform(put(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        save(token, 0, Map.of("title", "   ")).andExpect(status().isBadRequest());
        save(token, 0, Map.of("id", 0, "title", "Zero id")).andExpect(status().isBadRequest());
        save(token, 0, Map.of("title", "x".repeat(121))).andExpect(status().isBadRequest());
        save(token, 0, Map.of("title", "A", "detail", "x".repeat(2001))).andExpect(status().isBadRequest());
        assertThat(read(token).get("revision").asLong()).isZero();
    }
    @Test void acceptsTenItemsAndExactTextLimitsButRejectsEleven() throws Exception {
        String token = login("staff");
        Object[] ten = new Object[10];
        Arrays.fill(ten, Map.of("title", "x".repeat(120), "detail", "x".repeat(2000)));
        assertThat(body(save(token, 0, ten)).get("items").size()).isEqualTo(10);
        Object[] eleven = new Object[11]; Arrays.fill(eleven, Map.of("title", "A"));
        save(token, 1, eleven).andExpect(status().isBadRequest());
        assertThat(read(token).get("items").size()).isEqualTo(10);
    }
    @Test void authenticationAndBusinessRoleAreRequired() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":0,\"items\":[]}")).andExpect(status().isUnauthorized());
        String admin = login("admin");
        mvc.perform(get(URL).header("Authorization", admin)).andExpect(status().isForbidden());
        save(admin, 0).andExpect(status().isForbidden());
    }
    @Test void privateNoticeNoteCanBeCleared() throws Exception {
        String admin = login("admin"), staff = login("staff");
        var created = json.readTree(mvc.perform(post("/api/v1/admin/company-notices").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("title", "Notice", "content", "Full company detail"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        String url = "/api/v1/company-notices/" + created.get("id").asLong() + "/note";
        mvc.perform(put(url).header("Authorization", staff).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Saved\"}")).andExpect(status().isOk());
        mvc.perform(put(url).header("Authorization", staff).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value(""));
        mvc.perform(get(url.substring(0, url.length() - 5)).header("Authorization", staff)).andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value(""));
        mvc.perform(put(url).header("Authorization", staff).contentType(MediaType.APPLICATION_JSON).content("{\"content\":null}")).andExpect(status().isBadRequest());
        mvc.perform(put(url).header("Authorization", staff).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("content", "x".repeat(2001))))).andExpect(status().isBadRequest());
    }
    @Test void companyPanelIncludesFullNoticeDetail() throws Exception {
        String admin = login("admin"), staff = login("staff");
        mvc.perform(post("/api/v1/admin/company-notices").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("title", "Notice", "content", "Full company detail"))))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/panels/map").header("Authorization", staff)).andExpect(status().isOk())
            .andExpect(jsonPath("$.companyImportant[0].detail").value("Full company detail"));
    }
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentFirstSavesHaveOneWinnerAndOneConflict() throws Exception {
        String token = login("team_lead");
        long revision = read(token).get("revision").asLong();
        var start = new CountDownLatch(1); var ready = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        Callable<Integer> update = () -> { ready.countDown(); start.await();
            return save(token, revision, Map.of("title", "Concurrent")).andReturn().getResponse().getStatus(); };
        try {
            var a = executor.submit(update); var b = executor.submit(update);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
            assertThat(read(token).get("revision").asLong()).isEqualTo(revision + 1);
            assertThat(read(token).get("items").size()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }
}
