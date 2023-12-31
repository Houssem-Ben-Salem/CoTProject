package tn.supcom.repository;

import jakarta.nosql.mapping.Param;
import jakarta.nosql.mapping.Query;
import jakarta.nosql.mapping.Repository;
import tn.supcom.security.UserToken;

import java.util.Optional;

public interface UserTokenRepository extends Repository<UserToken,String> {


    Optional<UserToken> findByEmail(String  email ) ;
    @Query("select * from UserToken where tokens.token = @refreshToken")
    Optional<UserToken> findByRefreshToken(@Param("refreshToken") String token);

    @Query("select * from UserToken where tokens.accessToken.token = @accessToken")
    Optional<UserToken> findByAccessToken(@Param("accessToken") String token);

    // New method to delete a UserToken by its token
    @Query("delete from UserToken where tokens.token = @token")
    void deleteByToken(@Param("token") String token);



}
