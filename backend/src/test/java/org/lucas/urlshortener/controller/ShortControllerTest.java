package org.lucas.urlshortener.controller;

import org.junit.jupiter.api.Test;
import org.lucas.urlshortener.exception.GlobalExceptionHandler;
import org.lucas.urlshortener.exception.ShortUrlExpiredException;
import org.lucas.urlshortener.exception.ShortUrlNotFoundException;
import org.lucas.urlshortener.model.ShortUrl;
import org.lucas.urlshortener.service.ShortUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShortUrlController.class)
@Import(GlobalExceptionHandler.class)
public class ShortControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  ShortUrlService service;

  @Test
  void create_rejectsUrlWithSpace() throws Exception {
    mockMvc.perform(post("/api/urls")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"url\":\"https://example.com/a b\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors.url").exists());
  }

  @Test
  void create_rejectsNonPositiveExpiresInDays() throws Exception {
    mockMvc.perform(post("/api/urls")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"url\":\"https://example.com\",\"expiresInDays\":0}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors.expiresInDays").exists())
      .andExpect(jsonPath("$.errors.url").doesNotExist())
      .andExpect(jsonPath("$.errors.expiresInDays").value("must be greater than 0"));
  }

  @Test
  void create_rejectsHttpUrlWithoutHost() throws Exception {
    mockMvc.perform(post("/api/urls")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"url\":\"http:///x\"}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void create_returns201WithLocation() throws Exception {
    ShortUrl created = new ShortUrl();
    created.setCode("abc1234");
    created.setTargetUrl("https://example.com");
    when(service.create(any(), any())).thenReturn(created);

    mockMvc.perform(post("/api/urls")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"url\":\"https://example.com\"}"))
      .andExpect(status().isCreated())
      .andExpect(header().string("Location", "http://localhost/abc1234"))
      .andExpect(jsonPath("$.code").value("abc1234"));
  }

  @Test
  void create_rejectsNonHttpScheme() throws Exception {
    mockMvc.perform(post("/api/urls")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"url\":\"javascript:alert(1)\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors.url").exists());
  }

  @Test
  void redirect_returns302WithLocation() throws Exception {
    when(service.resolve("abc1234")).thenReturn("https://example.com");

    mockMvc.perform(get("/abc1234"))
      .andExpect(status().isFound())
      .andExpect(header().string("Location", "https://example.com"));
  }

  @Test
  void redirect_returns404WhenNotFound() throws Exception {
    when(service.resolve("nope")).thenThrow(new ShortUrlNotFoundException("nope"));

    mockMvc.perform(get("/nope")).andExpect(status().isNotFound());
  }

  @Test
  void redirect_returns410WhenExpired() throws Exception {
    when(service.resolve("old")).thenThrow(new ShortUrlExpiredException("old"));

    mockMvc.perform(get("/old")).andExpect(status().isGone());
  }

  @Test
  void create_returns400ProblemDetailForMalformedJson() throws Exception {
    mockMvc.perform(post("/api/urls")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{not json"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.detail").value("Malformed JSON request"));
  }

  @Test
  void create_rejectsTooLongUrl() throws Exception {
    mockMvc.perform(post("/api/urls")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"url\":\"https://example.com/" + "x".repeat(2100) + "\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors.url").value("must be at most 2048 characters"));
  }
}
