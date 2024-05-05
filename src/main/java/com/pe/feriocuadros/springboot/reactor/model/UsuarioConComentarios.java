package com.pe.feriocuadros.springboot.reactor.model;

public class UsuarioConComentarios {

	private Usuario usuario;
	
	private Comentarios comentarios;

	public UsuarioConComentarios(Usuario usuario, Comentarios comentarios) {
		this.usuario = usuario;
		this.comentarios = comentarios;
	}

	@Override
	public String toString() {
		return "UsuarioConComentarios [usuario=" + usuario + ", comentarios=" + comentarios + "]";
	}
	
	
}
