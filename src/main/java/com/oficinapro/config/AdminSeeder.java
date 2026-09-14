package com.oficinapro.config;

import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import com.oficinapro.security.role.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro ADMIN do SaaS a partir de variáveis de ambiente.
 *
 * Existe porque não há como criar usuário pela API sem estar autenticado, o que
 * tornaria a instalação inicial impossível. Grava direto pelo repositório, sem
 * passar pelo UsuarioService, justamente porque não existe usuário logado aqui.
 *
 * Só age se ADMIN_USERNAME e ADMIN_PASSWORD estiverem definidos e se ainda não
 * existir nenhum ADMIN, então reiniciar a aplicação nunca sobrescreve a senha
 * de um admin já existente.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String nome;

    public AdminSeeder(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${oficinapro.admin.username:}") String username,
                       @Value("${oficinapro.admin.password:}") String password,
                       @Value("${oficinapro.admin.nome:Administrador do SaaS}") String nome) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.nome = nome;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            return;
        }

        if (usuarioRepository.existsByRole(Role.ADMIN)) {
            log.debug("Já existe um ADMIN cadastrado, seeder ignorado");
            return;
        }

        if (usuarioRepository.existsByUsername(username)) {
            log.warn("Username '{}' já está em uso; ADMIN do SaaS não foi criado", username);
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome(nome);
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setOficina(null); // ADMIN do SaaS não tem filiação com oficina

        usuarioRepository.save(admin);

        log.info("ADMIN do SaaS '{}' criado. Troque a senha e remova ADMIN_PASSWORD do ambiente.", username);
    }
}
