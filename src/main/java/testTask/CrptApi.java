package testTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

public class CrptApi {
	private static final String API_KEY = "some_key";
	private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

	private final Semaphore semaphore;
	private final TimeUnit timeUnit;
	private final int requestLimit;
	private static AtomicInteger activeThreads = new AtomicInteger(0);

	public CrptApi(TimeUnit timeUnit, int requestLimit) {
		this.timeUnit = timeUnit;
		this.requestLimit = requestLimit;
		this.semaphore = new Semaphore(requestLimit);
	}

	private void createDocumentRequest(Document document, String signature) throws InterruptedException {

		semaphore.acquire();
		try {
			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
			Gson gson = new Gson();
			String json = gson.toJson(document);
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL))
					.header("Content-Type", "application/json").header("Authorization", "Bearer " + API_KEY)
					.POST(HttpRequest.BodyPublishers.ofString(json)).build();
			client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body)
					.thenAccept(response -> System.out.println("Document created: " + response)).join();
		} finally {
			semaphore.release();
			timeUnit.sleep(1);
		}

	}

	//Метод для подсчета активных потоков 
	private static void countActiveThreads() {
		int currentActiveThreads = activeThreads.incrementAndGet();
		System.out.println("Количество активных потоков: " + currentActiveThreads);
		activeThreads.decrementAndGet();
	}

	public static class Document {
		private String description;
		private String doc_id;
		private String doc_status;
		private String doc_type;
		private boolean importRequest;
		private String owner_inn;
		private String participant_inn;
		private String producer_inn;
		private String production_date;
		private String production_type;
		private Product[] products;
		private String reg_date;
		private String reg_number;

		public Document(String description, String doc_id, String doc_status, String doc_type, boolean importRequest,
				String owner_inn, String participant_inn, String producer_inn, String production_date,
				String production_type, Product[] products, String reg_date, String reg_number) {
			this.description = description;
			this.doc_id = doc_id;
			this.doc_status = doc_status;
			this.doc_type = doc_type;
			this.importRequest = importRequest;
			this.owner_inn = owner_inn;
			this.participant_inn = participant_inn;
			this.producer_inn = producer_inn;
			this.production_date = production_date;
			this.production_type = production_type;
			this.products = products;
			this.reg_date = reg_date;
			this.reg_number = reg_number;
		}

	}

	public static class Product {
		private String certificate_document;
		private String certificate_document_date;
		private String certificate_document_number;
		private String owner_inn;
		private String producer_inn;
		private String production_date;
		private String tnved_code;
		private String uit_code;
		private String uitu_code;

		public Product(String certificate_document, String certificate_document_date,
				String certificate_document_number, String owner_inn, String producer_inn, String production_date,
				String tnved_code, String uit_code, String uitu_code) {
			this.certificate_document = certificate_document;
			this.certificate_document_date = certificate_document_date;
			this.certificate_document_number = certificate_document_number;
			this.owner_inn = owner_inn;
			this.producer_inn = producer_inn;
			this.production_date = production_date;
			this.tnved_code = tnved_code;
			this.uit_code = uit_code;
			this.uitu_code = uitu_code;
		}
	}

	public static void main(String[] args) {
		CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 4);
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		for (int i = 0; i < 20; i++) {
			int finalI = i;
			executorService.submit(() -> {
				System.out.println("Поток " + finalI + " обратился");
				String signature = "some_signature_" + finalI;
				CrptApi.Product product = new CrptApi.Product("string_" + finalI, "2020-01-23", "string_" + finalI,
						"string_" + finalI, "string_" + finalI, "2020-01-23", "string_" + finalI, "string_" + finalI,
						"string_" + finalI);
				CrptApi.Product[] productsArray = new CrptApi.Product[] { product };
				CrptApi.Document document = new CrptApi.Document("string_" + finalI, "string_" + finalI,
						"string_" + finalI, "LP_INTRODUCE_GOODS", true, "string_" + finalI, "string_" + finalI,
						"string_" + finalI, "2020-01-23", "string_" + finalI, productsArray, "2020-01-23",
						"string_" + finalI);

				try {
					crptApi.createDocumentRequest(document, signature);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					countActiveThreads();
					System.out.println("Поток " + finalI + " обработал запрос");
				}
			});
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}