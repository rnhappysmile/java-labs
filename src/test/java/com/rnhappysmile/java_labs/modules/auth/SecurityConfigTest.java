package com.rnhappysmile.java_labs.modules.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.rnhappysmile.java_labs.auth.controller.AdminController;
import com.rnhappysmile.java_labs.auth.service.CustomOAuth2UserService;
import com.rnhappysmile.java_labs.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(AdminController.class) // 테스트할 컨트롤러 지정
@Import(SecurityConfig.class)      // 우리가 만든 설정 적용
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("인증되지 않은 사용자가 관리자 페이지 접근 시 302(리다이렉트) 혹은 401이 발생해야 한다")
    void guest_admin_access_fail() throws Exception {
        int status = mockMvc.perform(get("/admin/stat"))
                .andDo(print())
                .andReturn()
                .getResponse()
                .getStatus();
        System.out.println("Actual status: " + status); 
        assertThat(status).isIn(302, 303, 307, 308, 401, 403);
    }

    @Test
    @WithMockUser(roles = "USER") // ROLE_USER 권한 가짜 유저
    @DisplayName("일반 유저가 관리자 페이지 접근 시 403 Forbidden이 발생해야 한다")
    void user_admin_access_forbidden() throws Exception {
        mockMvc.perform(get("/admin/stat"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN") // ROLE_ADMIN 권한 가짜 유저
    @DisplayName("관리자는 관리자 페이지에 성공적으로 접근해야 한다")
    void admin_access_success() throws Exception {
        mockMvc.perform(get("/admin/stat"))
                .andExpect(status().isOk());
    }
}