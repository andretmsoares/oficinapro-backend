package com.oficinapro.service.auth;

import com.oficinapro.dto.auth.LoginRequestDTO;
import com.oficinapro.dto.auth.LoginResponseDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;

public interface AuthService {

  /**
   * @param clientIp IP de origem da requisição (já resolvido), usado só pelo controle de
   *     tentativas.
   */
  LoginResponseDTO login(LoginRequestDTO request, String clientIp);

  UsuarioResponseDTO usuarioLogado();

  /** Revoga todos os tokens do usuário autenticado (inclusive o da requisição atual). */
  void logout();
}
