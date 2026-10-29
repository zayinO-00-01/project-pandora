package com.projectpandora.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ProfileTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    String login(String password) throws Exception {
        var response=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username","staff","password",password))))
            .andExpect(status().isOk()).andReturn();
        return "Bearer "+json.readTree(response.getResponse().getContentAsString()).get("token").asText();
    }
    @Test void passwordRequiresCurrentSecretAndRevokesExistingSessions() throws Exception {
        String token=login("demo1234");
        mvc.perform(put("/api/v1/profile/password").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-password-123\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/profile/password").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"demo1234\",\"newPassword\":\"new-password-123\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/profile").header("Authorization",token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/profile").header("Authorization",login("new-password-123"))).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("staff"));
    }
    @Test void avatarValidatesImageContentAndSupportsRemoval() throws Exception {
        String token=login("demo1234");
        for(String bad:new String[]{"data:image/svg+xml;base64,PHN2Zz4=","data:image/png;base64,aGVsbG8="})
            mvc.perform(put("/api/v1/profile/avatar").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("avatarData",bad)))).andExpect(status().isBadRequest());
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        String data="data:image/png;base64,"+java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
        mvc.perform(put("/api/v1/profile/avatar").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("avatarData",data)))).andExpect(status().isOk()).andExpect(jsonPath("$.avatarData").value(data));
        mvc.perform(put("/api/v1/profile/avatar").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content("{\"avatarData\":\"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.avatarData").isEmpty());
    }
}
