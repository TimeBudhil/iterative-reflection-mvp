package iterative_reflection.mvp.career.internal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code text} is set for TEXT blocks; {@code url} (and optional {@code label}) for all other types. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentBlock(String type, String text, String url, String label) {
}
