package iterative_reflection.mvp.platform;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Import(PaginationHttpTest.ProbeController.class)
class PaginationHttpTest extends HttpIntegrationTest {

	@Test
	void defaultPageSizeIs20() {
		http.get().uri("/api/test-probe/items")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content.length()").isEqualTo(20)
				.jsonPath("$.page.size").isEqualTo(20)
				.jsonPath("$.page.number").isEqualTo(0)
				.jsonPath("$.page.totalElements").isEqualTo(250)
				.jsonPath("$.page.totalPages").isEqualTo(13);
	}

	@Test
	void requestedPageAndSizeAreHonoured() {
		http.get().uri("/api/test-probe/items?page=2&size=50")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content[0]").isEqualTo("item-100")
				.jsonPath("$.page.size").isEqualTo(50)
				.jsonPath("$.page.number").isEqualTo(2);
	}

	@Test
	void sizeAbove100IsClampedTo100() {
		http.get().uri("/api/test-probe/items?size=500")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content.length()").isEqualTo(100)
				.jsonPath("$.page.size").isEqualTo(100);
	}

	@RestController
	static class ProbeController {

		private static final List<String> ITEMS = IntStream.range(0, 250).mapToObj(i -> "item-" + i).toList();

		@GetMapping("/api/test-probe/items")
		Page<String> items(Pageable pageable) {
			int from = (int) Math.min(pageable.getOffset(), ITEMS.size());
			int to = Math.min(from + pageable.getPageSize(), ITEMS.size());
			return new PageImpl<>(ITEMS.subList(from, to), pageable, ITEMS.size());
		}

	}

}
