package com.oficinapro.service.usuario;

import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.model.Usuario;
import com.oficinapro.service.pessoaCrud.PessoaCrudService;

public interface UsuarioService
        extends PessoaCrudService<UsuarioRequestDTO, UsuarioUpdateRequestDTO, UsuarioResponseDTO, Usuario> {}