package com.oficinapro.service.auth;

import com.oficinapro.dto.auth.LoginRequestDTO;
import com.oficinapro.dto.auth.LoginResponseDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.model.Usuario;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           AuthenticatedUserProvider authenticatedUserProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * O DaoAuthenticationProvider converte "usuário inexistente" em BadCredentialsException,
     * de modo que a resposta é idêntica à de senha errada e não permite descobrir
     * quais usernames existem.
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        Usuario usuario = (Usuario) authentication.getPrincipal();

        return LoginResponseDTO.bearer(
                jwtService.gerarToken(usuario),
                jwtService.expiracao().toSeconds(),
                UsuarioResponseDTO.de(usuario));
    }

    @Override
    public UsuarioResponseDTO usuarioLogado() {
        return UsuarioResponseDTO.de(authenticatedUserProvider.getUsuarioAutenticado());
    }
}
