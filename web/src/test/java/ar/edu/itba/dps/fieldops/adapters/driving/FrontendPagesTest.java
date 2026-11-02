package ar.edu.itba.dps.fieldops.adapters.driving;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendPagesTest {
    @Autowired
    private MockMvc web;

    @Test
    void servesThePageThatLoadsTheApplication() throws Exception {
        web.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("js/main.js")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/styles.css", "/js/main.js", "/js/api.js", "/js/dom.js", "/js/messages.js", "/js/catalog.js",
            "/js/expedition-list.js", "/js/expedition.js", "/js/itinerary.js", "/js/planning.js", "/js/approval.js", "/js/proposals.js"})
    void servesEveryFileThePageNeeds(String path) throws Exception {
        web.perform(get(path)).andExpect(status().isOk());
    }
}
