package io.github.jangws1030.creatorsettlementapi.course;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class CatalogApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsCreators() throws Exception {
        mockMvc.perform(get("/api/creators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].id").value("creator-1"))
                .andExpect(jsonPath("$[1].id").value("creator-2"))
                .andExpect(jsonPath("$[2].id").value("creator-3"))
                .andExpect(jsonPath("$[3].id").value("creator-4"))
                .andExpect(jsonPath("$[4].id").value("creator-5"))
                .andExpect(jsonPath("$[5].id").value("creator-6"));
    }

    @Test
    void listsCoursesByCreator() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("creatorId", "creator-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("course-1"))
                .andExpect(jsonPath("$[1].id").value("course-2"));
    }

    @Test
    void listsAllCoursesWhenCreatorFilterIsMissing() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));
    }

    @Test
    void rejectsCoursesWhenCreatorDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("creatorId", "creator-missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Creator not found: creator-missing"));
    }
}
