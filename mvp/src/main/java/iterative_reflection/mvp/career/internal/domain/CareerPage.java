package iterative_reflection.mvp.career.internal.domain;

import java.util.List;

/** One page of a career's experience content, with its blocks in order. */
public record CareerPage(int number, String title, List<ContentBlock> blocks) {
}
