package com.pe.feriocuadros.springboot.reactor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.pe.feriocuadros.springboot.reactor.model.Comentarios;
import com.pe.feriocuadros.springboot.reactor.model.Usuario;
import com.pe.feriocuadros.springboot.reactor.model.UsuarioConComentarios;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringBootReactorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		ejemploContraPresionLimiteRate();
	}
	
	public void ejemploContraPresionLimiteRate() {
		Flux.range(1, 10)
		.log()
		.limitRate(2)
		.subscribe();
	}
	
	public void ejemploContraPresion() {
		Flux.range(1, 10)
		.log()
		.subscribe(new Subscriber<Integer>() {

			private Subscription s;
			private Integer limite = 5;
			private Integer consumido = 0;
			
			@Override
			public void onSubscribe(Subscription s) {
				this.s = s;
				s.request(limite);				
			}

			@Override
			public void onNext(Integer t) {
				log.info(t.toString());
				consumido++;
				if (consumido == limite) {
					consumido = 0;
					s.request(limite);
				}
			}

			@Override
			public void onError(Throwable t) {
				
			}

			@Override
			public void onComplete() {
								
			}
		});
	}
	
	public void ejemploIntervalCreate() throws InterruptedException {
		Flux.create(emmitter -> {
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				
				private Integer contador = 0;
				@Override
				public void run() {
					emmitter.next(++contador);		
					if (contador == 10) {
						timer.cancel();
						emmitter.complete();
					}
					
					if (contador == 5) {
						timer.cancel();
						emmitter.error(new InterruptedException("Errors, se ha detenido en 5"));
					}
				}
			}, 1000, 1000);
		})
		.subscribe(next -> log.info(next.toString()),
				error -> log.error(error.getMessage()),
				() -> log.info("Hemos Terminado"));
	}
	
	public void ejemploDelayElement() throws InterruptedException {
		Flux<Integer> rango = Flux.range(1, 12)
				.delayElements(Duration.ofSeconds(1))
				.doOnNext(i -> log.info(i.toString()));
		rango.subscribe(); //no se muestra nada en consola
		//rango.blockLast(); //bloquea hasta que se haya emitido el ultimo elemento (No recomendable)
		Thread.sleep(13000);
	}
	
	public void ejemploIntervalInfinite() throws InterruptedException {
		
		CountDownLatch latch = new CountDownLatch(1);
		
		Flux.interval(Duration.ofSeconds(1))
				.flatMap(i -> {					
					if (i >= 5) {
						return Flux.error(new InterruptedException("Solo hasta 5!"));
					}
					return Flux.just(i);						
				})
				.retry(2)
				.doOnTerminate(() -> {
					latch.countDown();
					log.info("doOnTerminate::latch::" + latch.getCount());
				})
				.map(i -> {
					String saludo = "Hola:: %d";
					return String.format(saludo, i);
				})				
				.subscribe(s -> log.info(s), e -> log.error(e.getMessage()));

		latch.await();
		
	}
	
	public void ejemploIntervalZipWith() {
		Flux<Integer> rango = Flux.range(1, 12);
		Flux<Long> delay = Flux.interval(Duration.ofSeconds(1));
		
		rango.zipWith(delay, (ra, re) -> ra)
			.doOnNext(i -> log.info(i.toString()))
			.blockLast();
		
	}
	
	public void ejemploZipWithRange2() {
		Flux.range(10, 3)
			.map(i -> (i * 2))
			.subscribe(texto -> log.info(texto.toString()));
		
	}
	
	public void ejemploZipWithRange() {
		Flux.just(1, 2, 3, 4, 5)
			.map(i -> (i * 10))
			.zipWith(Flux.range(1, 5), (multiplo, single) -> String.format("Item: %d, Item x 10: %d", single, multiplo))
			.subscribe(texto -> log.info(texto));
		
	}
	
	public void ejemploUsuarioComentarioZipWith2() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Fernando", "Cuadros"));
		Mono<Comentarios> comentariosUsuario = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola mano");
			comentarios.addComentario("Mañana futbol");
			return comentarios;
		});
		
		Mono<UsuarioConComentarios> usuarioConComentarios = usuarioMono
			.zipWith(comentariosUsuario)
			.map(tupla-> {
				Usuario usuario = tupla.getT1();
				Comentarios comentarios = tupla.getT2();
				return new UsuarioConComentarios(usuario, comentarios);
			});
		usuarioConComentarios.subscribe(uc -> log.info(uc.toString()));
	}
	
	public void ejemploUsuarioComentarioZipWith() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Fernando", "Cuadros"));
		Mono<Comentarios> comentariosUsuario = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola mano");
			comentarios.addComentario("Mañana futbol");
			return comentarios;
		});
		
		Mono<UsuarioConComentarios> usuarioConComentarios = usuarioMono
			.zipWith(comentariosUsuario, 
					(usuario, comentarios) -> new UsuarioConComentarios(usuario, comentarios));
		usuarioConComentarios.subscribe(uc -> log.info(uc.toString()));
	}
	
	public void ejemploUsuarioComentarioFlatMap() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Fernando", "Cuadros"));
		Mono<Comentarios> comentariosUsuario = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola mano");
			comentarios.addComentario("Mañana futbol");
			return comentarios;
		});
		
		usuarioMono
			.flatMap(u -> comentariosUsuario
					.map(c -> new UsuarioConComentarios(u, c)))
			.subscribe(uc -> log.info(uc.toString()));
	}

	public void ejemploCollectList() throws Exception {

		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Andres", "Santos"));
		usuariosList.add(new Usuario("Pedro", "Vargas"));
		usuariosList.add(new Usuario("Fernando", "Cuadros"));
		usuariosList.add(new Usuario("Luis", "Montes"));
		usuariosList.add(new Usuario("Juan", "LaRosa"));
		usuariosList.add(new Usuario("Bruce", "Lee"));
		usuariosList.add(new Usuario("Bruce", "Willis"));

		Flux.fromIterable(usuariosList)
			.collectList()
			.subscribe(lista -> {
				lista.forEach(item -> {
					log.info(item.toString());
				});
			});
	}
	
	public void ejemploToString() throws Exception {

		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Andres", "Santos"));
		usuariosList.add(new Usuario("Pedro", "Vargas"));
		usuariosList.add(new Usuario("Fernando", "Cuadros"));
		usuariosList.add(new Usuario("Luis", "Montes"));
		usuariosList.add(new Usuario("Juan", "LaRosa"));
		usuariosList.add(new Usuario("Bruce", "Lee"));
		usuariosList.add(new Usuario("Bruce", "Willis"));

		Flux.fromIterable(usuariosList).map(usuario -> {
			return usuario.getNombre().toUpperCase().concat(" ")
					.concat(usuario.getApellido().toUpperCase());
		})
		.flatMap(nombre -> {
			if (nombre.contains("bruce".toUpperCase())) {
				return Mono.just(nombre);
			} else
				return Mono.empty();
		}).map(nombre -> {
			return nombre.toLowerCase();
		}).subscribe(e -> log.info(e.toString()));
	}

	public void ejemploFlatMap() throws Exception {

		List<String> usuariosList = List.of("Andres Santos", "Pedro Vargas", "Fernando Cuadros", "Luis Montes",
				"Juan LaRosa", "Bruce Lee", "Bruce Willis");

		Flux.fromIterable(usuariosList).map(nombre -> {
			String[] datos = nombre.split(" ");
			return new Usuario(datos[0].toUpperCase(), datos[1].toUpperCase());
		}).flatMap(usuario -> {
			if (usuario.getNombre().equalsIgnoreCase("bruce")) {
				return Mono.just(usuario);
			} else
				return Mono.empty();
		}).map(usuario -> {
			usuario.setNombre(usuario.getNombre().toUpperCase());
			usuario.setApellido(usuario.getApellido().toUpperCase());
			return usuario;
		}).subscribe(e -> log.info(e.toString()));
	}

	public void ejemploIterable() throws Exception {

		List<String> usuariosList = List.of("Andres Santos", "Pedro Vargas", "Fernando Cuadros", "Luis Montes",
				"Juan LaRosa", "Bruce Lee", "Bruce Willis");

		Flux<String> nombres = Flux.fromIterable(usuariosList);
		// Flux<String> nombres = Flux.just("Andres Santos", "Pedro Vargas", "Fernando
		// Cuadros", "Luis Montes", "Juan LaRosa", "Bruce Lee", "Bruce Willis");

		Flux<Usuario> usuarios = nombres.map(nombre -> {
			String[] datos = nombre.split(" ");
			return new Usuario(datos[0].toUpperCase(), datos[1].toUpperCase());
		}).filter(usuario -> usuario.getNombre().equalsIgnoreCase("bruce")).doOnNext(usuario -> {
			if (usuario == null) {
				throw new RuntimeException("Nombres no pueden ser vacios");
			}
			System.out.println(usuario.getNombre().concat(" ").concat(usuario.getApellido()));

		}).map(usuario -> {
			String nombre = usuario.getNombre().toLowerCase();
			usuario.setNombre(nombre);
			return usuario;
		});

		usuarios.subscribe(e -> log.info(e.toString()), error -> log.error(error.getMessage()), new Runnable() {

			@Override
			public void run() {
				log.info("Ha finalizado la ejecucion del observable");

			}
		});
	}
}
