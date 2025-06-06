package org.example.projectmanager.integrationtest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.example.projectmanager.entity.Project;
import org.example.projectmanager.entity.Tasks;
import org.example.projectmanager.entity.Users;
import org.example.projectmanager.entity.TaskType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Klasa pomocnicza do przypisywania użytkowników
    public static class AssignUserRequest {
        private Long userId;

        public AssignUserRequest() {}

        public AssignUserRequest(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    @Test
    public void testFullProjectFlow() throws Exception {
        // 1. Create user
        Users user = new Users();
        user.setUsername("TestUser");
        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Users createdUser = objectMapper.readValue(userResponse, Users.class);

        // 2. Create project
        Project project = new Project();
        project.setName("TestProject");
        String projectResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Project createdProject = objectMapper.readValue(projectResponse, Project.class);
    }

    @Test
    public void testTaskLifecycle() throws Exception {
        // 1. Create project first
        Project project = new Project();
        project.setName("TaskProject");
        String projectResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Project createdProject = objectMapper.readValue(projectResponse, Project.class);

        Project projectForTask = new Project();
        projectForTask.setId(createdProject.getId());
        projectForTask.setName(createdProject.getName());

        // 2. Create task
        Tasks task = new Tasks();
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setTaskType(TaskType.IN_PROGRESS);
        task.setProject(projectForTask);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)));
    }

    @Test
    public void testGetAllUsers() throws Exception {
        // First create a user
        Users user = new Users();
        user.setUsername("UserForList");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated());

        // Then get all users
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").exists());
    }

    @Test
    public void testUpdateUser() throws Exception {
        // 1. Create user
        Users user = new Users();
        user.setUsername("OriginalName");
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();
        Users createdUser = objectMapper.readValue(response, Users.class);

        // 2. Update user
        createdUser.setUsername("UpdatedName");
        mockMvc.perform(put("/api/users/" + createdUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("UpdatedName"));
    }

    @Test
    public void testProjectCrud() throws Exception {
        // 1. Utwórz projekt
        Project project = new Project();
        project.setName("CRUDProject");

        MvcResult createResult = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isCreated())
                .andReturn();

        Project createdProject = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Project.class
        );

        // 2. Odczytaj projekt
        mockMvc.perform(get("/api/projects/" + createdProject.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CRUDProject"));

        // 3. Aktualizuj projekt
        Project updatedProject = new Project();
        updatedProject.setName("RenamedProject");

        mockMvc.perform(put("/api/projects/" + createdProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedProject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("RenamedProject"));

        // 4. Usuń projekt
        mockMvc.perform(delete("/api/projects/" + createdProject.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + createdProject.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAssignUserToProject() throws Exception {
        // 1. Create user
        Users user = new Users();
        user.setUsername("UserForAssignment");
        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Users createdUser = objectMapper.readValue(userResponse, Users.class);

        // 2. Create project
        Project project = new Project();
        project.setName("ProjectForAssignment");
        String projectResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Project createdProject = objectMapper.readValue(projectResponse, Project.class);

        // 3. Assign user to project
        AssignUserRequest assignRequest = new AssignUserRequest(createdUser.getId());
        mockMvc.perform(post("/api/projects/" + createdProject.getId() + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated());

        // 4. Verify user is in project's member list
        mockMvc.perform(get("/api/projects/" + createdProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectUsers[0].user.id").value(createdUser.getId()))
                .andExpect(jsonPath("$.projectUsers[0].user.username").value("UserForAssignment"));
    }
}