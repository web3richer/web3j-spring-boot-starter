package org.web3j.spring.autoconfigure;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.ApplicationContextTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;

@SpringBootTest
public class Web3jAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testEmptyClientAddress() throws Exception {
		verifyHttpConnection("", HttpService.DEFAULT_URL, HttpService.class);
	}

	@Test
	public void testHttpClient() throws Exception {
		verifyHttpConnection("https://localhost:12345", HttpService.class);
	}

	@Test
	public void testUnixIpcClient() throws IOException {
		Path path = Files.createTempFile("unix", "ipc");
		path.toFile().deleteOnExit();

		load(EmptyConfiguration.class, "web3j.client-address=" + path.toString());
	}

	@Test
	public void testWindowsIpcClient() throws IOException {
		// Windows uses a RandomAccessFile to access the named pipe, hence we can
		// initialise
		// the WindowsIPCService in web3j
		Path path = Files.createTempFile("windows", "ipc");
		path.toFile().deleteOnExit();

		System.setProperty("os.name", "windows");
		load(EmptyConfiguration.class, "web3j.client-address=" + path.toString());
	}

	@Test
	public void testHealthCheckIndicatorDown() {
		load(EmptyConfiguration.class, "web3j.client-address=");

		HealthIndicator web3jHealthIndicator = this.context.getBean(HealthIndicator.class);
		Health health = web3jHealthIndicator.health();
		assertEquals(health.getStatus(), Status.DOWN);
		if (!health.getDetails().get("error").toString()
				.startsWith("java.net.ConnectException: Failed to connect to localhost/")) {
			fail();
		}
	}

	private void verifyHttpConnection(String clientAddress, Class<? extends Service> cls) throws Exception {
		verifyHttpConnection(clientAddress, clientAddress, cls);
	}

	private void verifyHttpConnection(String clientAddress, String expectedClientAddress, Class<? extends Service> cls)
			throws Exception {
		load(EmptyConfiguration.class, "web3j.client-address=" + clientAddress);
		Web3j web3j = this.context.getBean(Web3j.class);

		Field web3jServiceField = JsonRpc2_0Web3j.class.getDeclaredField("web3jService");
		web3jServiceField.setAccessible(true);
		Web3jService web3jService = (Web3jService) web3jServiceField.get(web3j);

		assertTrue(cls.isInstance(web3jService));

		Field urlField = HttpService.class.getDeclaredField("url");
		urlField.setAccessible(true);
		String url = (String) urlField.get(web3jService);

		assertEquals(url, expectedClientAddress);
	}

	@Configuration
	static class EmptyConfiguration {
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		ApplicationContextTestUtils.closeAll(applicationContext);
		
		applicationContext.register(config);
		applicationContext.register(Web3jAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
	}

}
