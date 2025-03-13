package auth.security;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import auth.constant.InfoConstant;
import auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * @author fdse
 */
@Component("userDetailServiceImpl")
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService
{
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException
    {
        log.info("UsernamePasswordAuthenticationToken  username :" + s);
        return userRepository.findByUsername(s)
            .orElseThrow(() -> new UsernameNotFoundException(
                MessageFormat.format(InfoConstant.USER_NAME_NOT_FOUND_1, s)
            ));
    }
}
