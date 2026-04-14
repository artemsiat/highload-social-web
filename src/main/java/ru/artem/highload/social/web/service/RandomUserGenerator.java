package ru.artem.highload.social.web.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RandomUserGenerator {

    private final PasswordEncoder passwordEncoder;

    private static final String[] FIRST_NAMES_MALE = {
            "Александр", "Дмитрий", "Максим", "Сергей", "Андрей",
            "Алексей", "Артём", "Илья", "Кирилл", "Михаил",
            "Никита", "Матвей", "Роман", "Егор", "Арсений",
            "Иван", "Денис", "Евгений", "Тимофей", "Владислав",
            "Игорь", "Владимир", "Павел", "Руслан", "Марк",
            "Константин", "Тимур", "Олег", "Ярослав", "Антон",
            "Николай", "Глеб", "Данил", "Савелий", "Вадим",
            "Степан", "Юрий", "Богдан", "Георгий", "Лев",
            "Виталий", "Валерий", "Пётр", "Фёдор", "Григорий",
            "Леонид", "Геннадий", "Анатолий", "Борис", "Василий",
            "Семён", "Аркадий", "Вячеслав", "Эдуард", "Станислав",
            "Филипп", "Родион", "Платон", "Елисей", "Захар",
            "Давид", "Герман", "Макар", "Мирон", "Демид",
            "Всеволод", "Святослав", "Ратмир", "Назар", "Добрыня",
            "Архип", "Тихон", "Прохор", "Лука", "Емельян",
            "Харитон", "Остап", "Мстислав", "Радомир", "Велимир"
    };

    private static final String[] FIRST_NAMES_FEMALE = {
            "Анастасия", "Мария", "Анна", "Виктория", "Екатерина",
            "Наталья", "Марина", "Полина", "София", "Дарья",
            "Алиса", "Ксения", "Александра", "Елена", "Ольга",
            "Татьяна", "Ирина", "Юлия", "Светлана", "Кристина",
            "Валерия", "Вероника", "Алина", "Милана", "Ева",
            "Арина", "Василиса", "Ульяна", "Варвара", "Таисия",
            "Диана", "Елизавета", "Карина", "Маргарита", "Людмила",
            "Надежда", "Оксана", "Лариса", "Галина", "Тамара",
            "Зинаида", "Раиса", "Клавдия", "Антонина", "Нина",
            "Зоя", "Лидия", "Любовь", "Римма", "Инна",
            "Жанна", "Регина", "Снежана", "Яна", "Злата",
            "Стефания", "Агата", "Мирослава", "Есения", "Камила",
            "Аделина", "Эмилия", "Амелия", "Серафима", "Лилия",
            "Майя", "Нелли", "Пелагея", "Ангелина", "Анжелика",
            "Изабелла", "Виолетта", "Альбина", "Владислава", "Мелания",
            "Доминика", "Аврора", "Лада", "Забава", "Радмила"
    };

    private static final String[] LAST_NAMES_MALE = {
            "Иванов", "Смирнов", "Кузнецов", "Попов", "Васильев",
            "Петров", "Соколов", "Михайлов", "Новиков", "Фёдоров",
            "Морозов", "Волков", "Алексеев", "Лебедев", "Семёнов",
            "Егоров", "Павлов", "Козлов", "Степанов", "Николаев",
            "Орлов", "Андреев", "Макаров", "Никитин", "Захаров",
            "Зайцев", "Соловьёв", "Борисов", "Яковлев", "Григорьев",
            "Романов", "Воробьёв", "Сергеев", "Кузьмин", "Фролов",
            "Александров", "Дмитриев", "Королёв", "Гусев", "Киселёв",
            "Ильин", "Максимов", "Поляков", "Сорокин", "Виноградов",
            "Ковалёв", "Белов", "Медведев", "Антонов", "Тарасов",
            "Жуков", "Баранов", "Филиппов", "Комаров", "Давыдов",
            "Беляев", "Герасимов", "Богданов", "Осипов", "Сидоров",
            "Матвеев", "Титов", "Марков", "Миронов", "Крылов",
            "Куликов", "Карпов", "Власов", "Мельников", "Денисов",
            "Гаврилов", "Тихонов", "Казаков", "Афанасьев", "Данилов",
            "Пономарёв", "Калинин", "Чернов", "Хохлов", "Большаков",
            "Суворов", "Миляев", "Туров", "Ушаков", "Шестаков",
            "Люлин", "Кулагин", "Архипов", "Лавров", "Щербаков",
            "Калашников", "Корнилов", "Шувалов", "Трофимов", "Логинов",
            "Лукьянов", "Рябов", "Горбунов", "Блинов", "Сычёв"
    };

    private static final String[] LAST_NAMES_FEMALE = {
            "Иванова", "Смирнова", "Кузнецова", "Попова", "Васильева",
            "Петрова", "Соколова", "Михайлова", "Новикова", "Фёдорова",
            "Морозова", "Волкова", "Алексеева", "Лебедева", "Семёнова",
            "Егорова", "Павлова", "Козлова", "Степанова", "Николаева",
            "Орлова", "Андреева", "Макарова", "Никитина", "Захарова",
            "Зайцева", "Соловьёва", "Борисова", "Яковлева", "Григорьева",
            "Романова", "Воробьёва", "Сергеева", "Кузьмина", "Фролова",
            "Александрова", "Дмитриева", "Королёва", "Гусева", "Киселёва",
            "Ильина", "Максимова", "Полякова", "Сорокина", "Виноградова",
            "Ковалёва", "Белова", "Медведева", "Антонова", "Тарасова",
            "Жукова", "Баранова", "Филиппова", "Комарова", "Давыдова",
            "Беляева", "Герасимова", "Богданова", "Осипова", "Сидорова",
            "Матвеева", "Титова", "Маркова", "Миронова", "Крылова",
            "Куликова", "Карпова", "Власова", "Мельникова", "Денисова",
            "Гаврилова", "Тихонова", "Казакова", "Афанасьева", "Данилова",
            "Пономарёва", "Калинина", "Чернова", "Хохлова", "Большакова",
            "Суворова", "Миляева", "Турова", "Ушакова", "Шестакова",
            "Люлина", "Кулагина", "Архипова", "Лаврова", "Щербакова",
            "Калашникова", "Корнилова", "Шувалова", "Трофимова", "Логинова",
            "Лукьянова", "Рябова", "Горбунова", "Блинова", "Сычёва"
    };

    private static final String[] CITIES = {
            "Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань",
            "Нижний Новгород", "Челябинск", "Самара", "Омск", "Ростов-на-Дону",
            "Уфа", "Красноярск", "Воронеж", "Пермь", "Волгоград",
            "Краснодар", "Саратов", "Тюмень", "Тольятти", "Ижевск",
            "Барнаул", "Ульяновск", "Иркутск", "Хабаровск", "Ярославль",
            "Владивосток", "Махачкала", "Томск", "Оренбург", "Кемерово",
            "Рязань", "Астрахань", "Пенза", "Липецк", "Калининград",
            "Тула", "Курск", "Сочи", "Ставрополь", "Белгород",
            "Калуга", "Сургут", "Брянск", "Чита", "Иваново",
            "Владимир", "Архангельск", "Смоленск", "Курган", "Якутск",
            "Грозный", "Мурманск", "Тверь", "Кострома", "Вологда",
            "Петрозаводск", "Нальчик", "Сыктывкар", "Саранск", "Тамбов"
    };

    private static final String[] INTERESTS = {
            "футбол", "хоккей", "баскетбол", "плавание", "бег",
            "чтение", "музыка", "кино", "путешествия", "фотография",
            "кулинария", "рисование", "программирование", "шахматы", "йога",
            "велоспорт", "танцы", "рыбалка", "настольные игры", "садоводство",
            "теннис", "волейбол", "лыжи", "сноуборд", "скалолазание",
            "автомобили", "мотоциклы", "авиамоделирование", "робототехника", "3D-печать",
            "вышивание", "вязание", "гончарное дело", "каллиграфия", "оригами",
            "астрономия", "история", "философия", "психология", "иностранные языки"
    };

    private static final Map<Character, String> TRANSLIT = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"),
            Map.entry('г', "g"), Map.entry('д', "d"), Map.entry('е', "e"),
            Map.entry('ё', "yo"), Map.entry('ж', "zh"), Map.entry('з', "z"),
            Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"),
            Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"),
            Map.entry('с', "s"), Map.entry('т', "t"), Map.entry('у', "u"),
            Map.entry('ф', "f"), Map.entry('х', "kh"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sch"),
            Map.entry('ъ', ""), Map.entry('ы', "y"), Map.entry('ь', ""),
            Map.entry('э', "e"), Map.entry('ю', "yu"), Map.entry('я', "ya")
    );

    public String computeSharedPasswordHash() {
        return passwordEncoder.encode("test_password");
    }

    public List<Object[]> generateBatch(int size, String passwordHash) {
        List<Object[]> batch = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            batch.add(generateRandomUser(passwordHash));
        }
        return batch;
    }

    private Object[] generateRandomUser(String passwordHash) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean isMale = random.nextBoolean();

        String firstName = isMale
                ? FIRST_NAMES_MALE[random.nextInt(FIRST_NAMES_MALE.length)]
                : FIRST_NAMES_FEMALE[random.nextInt(FIRST_NAMES_FEMALE.length)];

        String lastName = isMale
                ? LAST_NAMES_MALE[random.nextInt(LAST_NAMES_MALE.length)]
                : LAST_NAMES_FEMALE[random.nextInt(LAST_NAMES_FEMALE.length)];

        String login = generateLogin(firstName, lastName, random);
        String gender = isMale ? "male" : "female";
        String city = CITIES[random.nextInt(CITIES.length)];

        int interestCount = random.nextInt(1, 4);
        StringBuilder interests = new StringBuilder();
        for (int i = 0; i < interestCount; i++) {
            if (i > 0) interests.append(", ");
            interests.append(INTERESTS[random.nextInt(INTERESTS.length)]);
        }

        LocalDate birthDate = LocalDate.of(
                random.nextInt(1960, 2006),
                random.nextInt(1, 13),
                random.nextInt(1, 29)
        );

        return new Object[]{
                login, passwordHash, firstName, lastName,
                Date.valueOf(birthDate), gender, interests.toString(), city
        };
    }

    private String generateLogin(String firstName, String lastName, ThreadLocalRandom random) {
        String first = transliterate(firstName).toLowerCase();
        String last = transliterate(lastName).toLowerCase();
        int num = random.nextInt(1, 10000);
        String separator = switch (random.nextInt(4)) {
            case 0 -> ".";
            case 1 -> "_";
            case 2 -> "-";
            default -> "";
        };

        String base = random.nextBoolean()
                ? first + separator + last
                : first + separator + last + num;

        return base + "_" + random.nextInt(100_000, 999_999);
    }

    private static String transliterate(String cyrillic) {
        StringBuilder sb = new StringBuilder(cyrillic.length());
        for (char c : cyrillic.toCharArray()) {
            String mapped = TRANSLIT.get(Character.toLowerCase(c));
            sb.append(mapped != null ? mapped : c);
        }
        return sb.toString();
    }
}
