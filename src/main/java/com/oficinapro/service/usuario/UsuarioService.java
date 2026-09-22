package com.oficinapro.service.usuario;

import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.dto.usuario.UsuarioMeUpdateRequestDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.model.Usuario;
import com.oficinapro.service.pessoaCrud.PessoaCrudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService
    extends PessoaCrudService<
        UsuarioRequestDTO, UsuarioUpdateRequestDTO, UsuarioResponseDTO, Usuario> {
  /** Todos os usuários da plataforma (ADMIN) ou só os da própria oficina (GERENTE). */
  Page<UsuarioResponseDTO> listar(Pageable pageable);

  UsuarioResponseDTO atualizarMe(UsuarioMeUpdateRequestDTO request);
}
