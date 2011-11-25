package com.tomakehurst.wiremock.mapping;

import static com.tomakehurst.wiremock.http.RequestMethod.DELETE;
import static com.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.tomakehurst.wiremock.testsupport.MappingJsonSamples.BASIC_MAPPING_REQUEST_WITH_RESPONSE_HEADER;
import static com.tomakehurst.wiremock.testsupport.MockRequestBuilder.aRequest;
import static com.tomakehurst.wiremock.testsupport.RequestResponseMappingBuilder.aMapping;
import static com.tomakehurst.wiremock.testsupport.WireMatchers.jsonEqualTo;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.tomakehurst.wiremock.http.RequestMethod;
import com.tomakehurst.wiremock.verification.RequestJournal;

@RunWith(JMock.class)
public class AdminRequestHandlerTest {
	
	private Mockery context;
	private Mappings mappings;
	private RequestJournal requestJournal;
	private GlobalSettingsHolder globalSettingsHolder;
	
	private AdminRequestHandler handler;
	
	
	@Before
	public void init() {
		context = new Mockery();
		mappings = context.mock(Mappings.class);
		requestJournal = context.mock(RequestJournal.class);
		globalSettingsHolder = new GlobalSettingsHolder();
		
		handler = new AdminRequestHandler(mappings, requestJournal, globalSettingsHolder);
	}
	
	@Test
	public void shouldAddNewMappingWhenCalledWithValidRequest() {
		Request request = aRequest(context)
			.withUrl("/mappings/new")
			.withMethod(POST)
			.withBody(BASIC_MAPPING_REQUEST_WITH_RESPONSE_HEADER)
			.build();
		
		context.checking(new Expectations() {{
			one(mappings).addMapping(aMapping()
					.withMethod(RequestMethod.GET)
					.withUrl("/a/registered/resource")
					.withResponseStatus(401)
					.withResponseBody("Not allowed!")
					.withHeader("Content-Type", "text/plain")
					.build());
		}});
		
		Response response = handler.handle(request);
		
		assertThat(response.getStatus(), is(HTTP_CREATED));
	}
	
	@Test
	public void shouldClearMappingsAndJournalWhenResetCalled() {
		Request request = aRequest(context)
			.withUrl("/reset")
			.withMethod(POST)
			.build();
		
		context.checking(new Expectations() {{
			one(mappings).reset();
			one(requestJournal).reset();
		}});
		
		Response response = handler.handle(request);
		
		assertThat(response.getStatus(), is(HTTP_OK));
	}
	
	private static final String REQUEST_PATTERN_SAMPLE = 
		"{												\n" +
		"	\"method\": \"DELETE\",						\n" +
		"	\"url\": \"/some/resource\"					\n" +
		"}												";
	
	@Test
	public void shouldReturnCountOfMatchingRequests() {
		context.checking(new Expectations() {{
			RequestPattern requestPattern = new RequestPattern(DELETE, "/some/resource");
			allowing(requestJournal).countRequestsMatching(requestPattern); will(returnValue(5));
		}});
		
		Response response = handler.handle(aRequest(context)
				.withUrl("/requests/count")
				.withMethod(POST)
				.withBody(REQUEST_PATTERN_SAMPLE)
				.build());
		
		assertThat(response.getStatus(), is(HTTP_OK));
		assertThat(response.getBody(), jsonEqualTo("{ \"count\": 5 }"));
	}
	
	private static final String GLOBAL_SETTINGS_JSON =
		"{												\n" +
		"	\"fixedDelay\": 2000						\n" +
		"}												";
	
	@Test
	public void shouldUpdateGlobalSettings() {
		handler.handle(aRequest(context)
				.withUrl("/settings")
				.withMethod(POST)
				.withBody(GLOBAL_SETTINGS_JSON)
				.build());
		
		assertThat(globalSettingsHolder.get().getFixedDelay(), is(2000));
	}
	
}