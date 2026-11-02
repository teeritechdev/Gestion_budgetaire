package com.budget.backend.security.security;

import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec le nom : " + username));

        if (!Boolean.TRUE.equals(utilisateur.getActif())
                || utilisateur.getSalarie() == null
                || !Boolean.TRUE.equals(utilisateur.getSalarie().getActif())) {
            throw new DisabledException("Ce compte utilisateur ou son dossier salarié est désactivé.");
        }
        if (utilisateur.getRole() == null) {
            throw new DisabledException("Aucun rôle n'est attribué à ce compte utilisateur.");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        // Ajout du rôle principal (ex: ROLE_AGENT, ROLE_DG)
        authorities.add(new SimpleGrantedAuthority(utilisateur.getRole().getCode()));

        // Ajout de toutes les permissions associées au rôle
        utilisateur.getRole().getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.getCode()))
        );

        return new User(
                utilisateur.getUsername(),
                utilisateur.getMotDePasseHash(),
                utilisateur.getActif(),
                true, true, true,
                authorities
        );
    }
}