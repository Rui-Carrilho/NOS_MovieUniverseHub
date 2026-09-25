package com.movieuniverse.hub.batch1;

import com.movieuniverse.hub.rating.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RatingControllerTest {
    @Test void rejectsFractionalRatingInsteadOfSilentlyTruncatingIt() {
        var service = mock(RatingService.class);
        var controller = new RatingController(service, mock(ScoreService.class));
        assertThatThrownBy(() -> controller.save(27205, 1,
                new RatingController.RatingRequest(new BigDecimal("8.5"))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(service);
    }

    @Test void passesExactIntegerToService() {
        var service = mock(RatingService.class);
        var controller = new RatingController(service, mock(ScoreService.class));
        assertThat(controller.save(27205, 1,
                new RatingController.RatingRequest(new BigDecimal("8"))).getStatusCode().value())
                .isEqualTo(204);
        verify(service).save(1, 27205, 8);
    }
}
