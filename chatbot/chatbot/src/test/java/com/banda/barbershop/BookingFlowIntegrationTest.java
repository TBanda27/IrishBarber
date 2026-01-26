package com.banda.barbershop;

import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.repository.BookingRepository;
import com.banda.barbershop.service.ConversationStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ConversationStateService conversationStateService;

    private final String TEST_USER_PHONE_NUMBER = "whatsapp:+1234567890";

    @BeforeEach
    void setUp() {
        // Clear conversation state for the test user before each test
        conversationStateService.deleteConversation(TEST_USER_PHONE_NUMBER);
    }

    @Test
    void testFullBookingFlow() throws Exception {
        // Step 1: User sends initial message to start booking
        mockMvc.perform(post("/whatsapp")
                        .param("From", TEST_USER_PHONE_NUMBER)
                        .param("Body", "1")) // Assuming "1" is for "Book Appointment"
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please choose a service by replying with the corresponding number")));

        // Step 2: User selects a service
        mockMvc.perform(post("/whatsapp")
                        .param("From", TEST_USER_PHONE_NUMBER)
                        .param("Body", "1")) // Assuming "1" is for the first service
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("You've selected")));

        // Step 3: User selects a barber
        mockMvc.perform(post("/whatsapp")
                        .param("From", TEST_USER_PHONE_NUMBER)
                        .param("Body", "1")) // Assuming "1" is for the first barber
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("You've selected the barber")));

        // Step 4: User selects a date
        // For simplicity, let's assume the user picks the first available date.
        // In a real scenario, this would be more complex.
        mockMvc.perform(post("/whatsapp")
                        .param("From", TEST_USER_PHONE_NUMBER)
                        .param("Body", "1")) // Assuming "1" is for the first available date
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please select an available time")));

        // Step 5: User selects a time
        mockMvc.perform(post("/whatsapp")
                        .param("From", TEST_USER_PHONE_NUMBER)
                        .param("Body", "1")) // Assuming "1" is for the first available time
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please confirm your booking details")));

        // Step 6: User confirms the booking
        mockMvc.perform(post("/whatsapp")
                        .param("From", TEST_USER_PHONE_NUMBER)
                        .param("Body", "1")) // Assuming "1" is for "Confirm"
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your booking is confirmed!")));

        // Verify that the booking was saved to the database
        Booking booking = bookingRepository.findAll().get(0);
        assertThat(booking).isNotNull();
        assertThat(booking.getCustomerPhone()).isEqualTo(TEST_USER_PHONE_NUMBER.substring(9));
    }
}
