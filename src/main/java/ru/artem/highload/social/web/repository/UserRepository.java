package ru.artem.highload.social.web.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.artem.highload.social.web.entity.UserEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<UserEntity> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        UserEntity user = new UserEntity();
        user.setId(rs.getLong("id"));
        user.setLogin(rs.getString("login"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setBirthDate(rs.getDate("birth_date").toLocalDate());
        user.setGender(rs.getString("gender"));
        user.setInterests(rs.getString("interests"));
        user.setCity(rs.getString("city"));
        user.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return user;
    };

    public Long createUser(String login, String passwordHash, String firstName, String lastName,
                           java.time.LocalDate birthDate, String gender, String interests, String city) {
        String sql = """
                INSERT INTO users (login, password_hash, first_name, last_name, birth_date, gender, interests, city)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        return jdbcTemplate.queryForObject(sql, Long.class,
                login, passwordHash, firstName, lastName,
                java.sql.Date.valueOf(birthDate), gender, interests, city);
    }

    public Optional<UserEntity> findByLogin(String login) {
        String sql = "SELECT * FROM users WHERE login = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, login).stream().findFirst();
    }

    public Optional<UserEntity> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    public List<UserEntity> searchByFirstNameAndLastNamePrefix(String firstNamePrefix, String lastNamePrefix) {
        String sql = """
                SELECT * FROM users
                WHERE first_name LIKE ? AND last_name LIKE ?
                ORDER BY id
                """;
        return jdbcTemplate.query(sql, ROW_MAPPER, firstNamePrefix + "%", lastNamePrefix + "%");
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count != null ? count : 0;
    }

    public void batchInsertUsers(List<Object[]> batch) {
        String sql = """
                INSERT INTO users (login, password_hash, first_name, last_name, birth_date, gender, interests, city)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.batchUpdate(sql, batch);
    }
}
