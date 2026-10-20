package com.budget.backend.security.security;

import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email : " + email));

        if (!utilisateur.getActif()) {
            throw new RuntimeException("Ce compte utilisateur est désactivé.");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        // Ajout du rôle principal (ex: ROLE_AGENT, ROLE_DG)
        authorities.add(new SimpleGrantedAuthority(utilisateur.getRole().getCode()));

        // Ajout de toutes les permissions associées au rôle
        utilisateur.getRole().getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.getCode()))
        );

        return new User(
                utilisateur.getEmail(),
                utilisateur.getMotDePasseHash(),
                utilisateur.getActif(),
                true, true, true,
                authorities
        );
    }
}