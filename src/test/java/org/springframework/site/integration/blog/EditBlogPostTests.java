package org.springframework.site.integration.blog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.context.initializer.ConfigFileApplicationContextInitializer;
import org.springframework.site.blog.Post;
import org.springframework.site.blog.PostBuilder;
import org.springframework.site.blog.PostCategory;
import org.springframework.site.blog.PostRepository;
import org.springframework.test.configuration.ElasticsearchStubConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = ElasticsearchStubConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class EditBlogPostTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Autowired
	private PostRepository postRepository;

	private Post post;
	private String originalTitle = "Original Title";
	private String originalContent = "Original Content";
	private PostCategory originalCategory = PostCategory.ENGINEERING;

	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		postRepository.deleteAll();
		post = PostBuilder.post()
				.title(originalTitle)
				.rawContent(originalContent)
				.category(originalCategory).build();
		postRepository.save(post);
	}

	@After
	public void tearDown() throws Exception {
		postRepository.deleteAll();
	}

	@Test
	public void getEditBlogPage() throws Exception {
		this.mockMvc.perform(get("/admin" + post.getPath() + "/edit"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(containsString("Edit Blog Post")));
	}

	@Test
	public void redirectToPublishedPostAfterUpdate() throws Exception {
		MockHttpServletRequestBuilder editPostRequest = createEditPostRequest();

		this.mockMvc.perform(editPostRequest)
				.andExpect(status().isFound())
				.andExpect(new ResultMatcher() {
					@Override
					public void match(MvcResult result) {
						String redirectedUrl = result.getResponse().getRedirectedUrl();
						assertThat(redirectedUrl, startsWith("/blog/" + post.getId() + "-new-title"));
					}
				});
	}

	@Test
	public void updatePostSavesNewValues() throws Exception {
		MockHttpServletRequestBuilder editPostRequest = createEditPostRequest();

		this.mockMvc.perform(editPostRequest);
		Post updatedPost = postRepository.findOne(post.getId());

		assertEquals("New Title", updatedPost.getTitle());
		assertEquals("New Content", updatedPost.getRawContent());
		assertEquals(PostCategory.NEWS_AND_EVENTS, updatedPost.getCategory());
		assertEquals(false, updatedPost.isDraft());
	}

	private MockHttpServletRequestBuilder createEditPostRequest() {
		MockHttpServletRequestBuilder editPostRequest = put("/admin"+ post.getPath());
		editPostRequest.param("title", "New Title");
		editPostRequest.param("content", "New Content");
		editPostRequest.param("category", PostCategory.NEWS_AND_EVENTS.name());
		editPostRequest.param("draft", "false");
		return editPostRequest;
	}

}