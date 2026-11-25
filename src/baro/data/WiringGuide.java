package baro.data;


import static baro.Tr.tr;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Как работают сигнальные компоненты и датчики (по исходному коду игры, Items/Components/Signal)
 * и типовые схемы. Разъёмы каждого предмета берутся из файлов игры, здесь — только пояснения.
 */
public final class WiringGuide {
    /** Пояснение к компоненту: что делает и настройки (название в игре — смысл). */
    public record Entry(String what, List<String> settings) { }

    /** Типовая схема: задача и шаги. */
    public record Circuit(String title, String goal, List<String> steps) { }

    private static final String BOOL_SETTINGS_OUT = tr("Output — что отправить, когда условие выполнено (по умолчанию «1»)");
    private static final String BOOL_SETTINGS_FALSE = tr("False Output — что отправить, когда не выполнено (по умолчанию «0»; пусто — ничего не отправлять)");
    private static final String TIMEFRAME = tr("Time Frame — сколько секунд помнится сигнал на входе; 0 — сигналы должны прийти одновременно");

    private static final Map<String, Entry> ENTRIES = Map.ofEntries(
            Map.entry("andcomponent", new Entry(tr("Логическое «И»: выдаёт Output, когда на ОБА входа пришёл ненулевой сигнал (в пределах Time Frame)."),
                    List.of(BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE, TIMEFRAME, tr("Вход «назначить исходящее значение» меняет Output сигналом")))),
            Map.entry("orcomponent", new Entry(tr("Логическое «ИЛИ»: выдаёт Output, когда хотя бы на одном входе ненулевой сигнал."),
                    List.of(BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE, TIMEFRAME))),
            Map.entry("xorcomponent", new Entry(tr("Исключающее «ИЛИ»: Output, когда ненулевой сигнал ровно на одном входе из двух."),
                    List.of(BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE, TIMEFRAME))),
            Map.entry("notcomponent", new Entry(tr("Инвертор: «0» или пустой сигнал превращает в «1», любой другой — в «0». ")
                    + tr("Пока сигнала на входе нет, выдаёт «1» только с включённым Continuous Output."),
                    List.of(tr("Continuous Output — выдавать «1», когда на вход ничего не приходит")))),
            Map.entry("equalscomponent", new Entry(tr("Сравнение: Output, когда сигналы на двух входах равны (как строки)."),
                    List.of(BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE, TIMEFRAME))),
            Map.entry("greatercomponent", new Entry(tr("«Больше»: Output, когда число на входе 1 больше числа на входе 2."),
                    List.of(BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE, TIMEFRAME))),
            Map.entry("signalcheckcomponent", new Entry(tr("Проверка сигнала: Output, если пришедший сигнал равен Target Signal, иначе False Output. ")
                    + tr("Удобно превращать числа и слова в «1/0» или наоборот."),
                    List.of(tr("Target Signal — с чем сравнивать"), BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE, tr("Вход «назначить целевой сигнал» меняет Target Signal")))),
            Map.entry("regexfindcomponent", new Entry(tr("Поиск по регулярному выражению: Output, если сигнал подходит под Expression. ")
                    + tr("Может выдавать найденную часть текста (группу захвата)."),
                    List.of(tr("Expression — регулярное выражение"), BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE,
                            tr("Use Capture Group — выдавать найденную группу вместо Output"), tr("Continuous Output — повторять результат, пока нет нового сигнала")))),
            Map.entry("memorycomponent", new Entry(tr("Память: хранит значение и всё время выдаёт его. Сигнал на «сигнал_в» записывается, пока запись разрешена; ")
                    + tr("«1» на «сигнал_запись» (или «закрепить_состояние») разрешает запись, «0» — запрещает."),
                    List.of(tr("Value — хранимое значение"), tr("Writeable — можно ли менять значение сигналами")))),
            Map.entry("delaycomponent", new Entry(tr("Задержка: пересылает сигналы на выход через Delay секунд."),
                    List.of(tr("Delay — задержка в секундах"), tr("Reset When Signal Received — сбрасывать очередь при каждом новом сигнале"),
                            tr("Reset When Different Signal Received — сбрасывать, когда сигнал изменился")))),
            Map.entry("relaycomponent", new Entry(tr("Реле: пропускает энергию и сигналы с «сигнал_в N» на «сигнал_из N», пока включено. ")
                    + tr("Включается и выключается сигналами «переключить состояние» / «назначить состояние»."),
                    List.of(tr("Is On — включено ли"), tr("Max Power — сколько энергии может пропустить")))),
            Map.entry("oscillatorcomponent", new Entry(tr("Генератор: непрерывно выдаёт импульсы или волну (синус, прямоугольник, пила, треугольник)."),
                    List.of(tr("Output Type — форма сигнала (Pulse — импульсы «1»)"), tr("Frequency — частота в герцах")))),
            Map.entry("concatcomponent", new Entry(tr("Склеивает два сигнала в одну строку (с разделителем)."), List.of(tr("Separator — что вставить между ними")))),
            Map.entry("colorcomponent", new Entry(tr("Собирает цвет «R,G,B,A» из четырёх чисел (или из HSV) — для ламп."), List.of(tr("Use HSV — входы как оттенок, насыщенность, яркость")))),
            Map.entry("multiplexercomponent", new Entry(tr("Мультиплексор: передаёт на выход сигнал с того входа, номер которого задан на «выбрать вход»."), List.of())),
            Map.entry("demultiplexercomponent", new Entry(tr("Демультиплексор: отправляет входной сигнал на выход с номером, заданным на «выбрать выход»."), List.of())),
            Map.entry("wificomponent", new Entry(tr("Беспроводная связь: пересылает сигналы другим Wi-Fi компонентам на том же канале (в том числе в чат из гарнитуры)."),
                    List.of(tr("Channel — канал"), tr("Range — дальность")))),
            Map.entry("addercomponent", new Entry(tr("Сложение: вход 1 + вход 2."), List.of(TIMEFRAME, tr("Clamp Min / Clamp Max — пределы результата")))),
            Map.entry("subtractcomponent", new Entry(tr("Вычитание: вход 1 − вход 2."), List.of(TIMEFRAME, tr("Clamp Min / Clamp Max — пределы результата")))),
            Map.entry("multiplycomponent", new Entry(tr("Умножение: вход 1 × вход 2."), List.of(TIMEFRAME, tr("Clamp Min / Clamp Max — пределы результата")))),
            Map.entry("dividecomponent", new Entry(tr("Деление: вход 1 / вход 2."), List.of(TIMEFRAME, tr("Clamp Min / Clamp Max — пределы результата")))),
            Map.entry("exponentiationcomponent", new Entry(tr("Возведение в степень: вход в степени Exponent."), List.of(tr("Exponent — показатель степени")))),
            Map.entry("modulocomponent", new Entry(tr("Остаток от деления на Modulus."), List.of(tr("Modulus — делитель")))),
            Map.entry("functioncomponent", new Entry(tr("Функция одного числа: округление, модуль, корень, факториал — смотря какой это компонент."), List.of())),
            Map.entry("trigonometricfunctioncomponent", new Entry(tr("Тригонометрия: синус, косинус, тангенс и обратные (в градусах)."), List.of())),
            Map.entry("motionsensor", new Entry(tr("Датчик движения: Output, когда в зоне кто-то движется, иначе False Output."),
                    List.of(tr("Target — кого замечать (людей, монстров, предметы, всех)"), tr("Range X / Range Y — размер зоны"), tr("Detect Offset — смещение зоны"),
                            tr("Minimum Velocity — с какой скорости считать движением"), tr("Ignore Dead — не замечать тела"), BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE))),
            Map.entry("waterdetector", new Entry(tr("Датчик воды: Output, когда датчик под водой; ещё выдаёт процент воды в помещении и сигнал высокого давления."),
                    List.of(BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE))),
            Map.entry("oxygendetector", new Entry(tr("Датчик кислорода: выдаёт процент кислорода в помещении (0–100) и сигнал «мало кислорода»."), List.of())),
            Map.entry("smokedetector", new Entry(tr("Датчик дыма: Output, когда рядом пожар."), List.of(BOOL_SETTINGS_OUT, BOOL_SETTINGS_FALSE))),
            Map.entry("lightcomponent", new Entry(tr("Свет: «переключить состояние» включает и выключает, «назначить состояние» — «1»/«0», «назначить цвет» — «R,G,B,A»."), List.of())),
            Map.entry("reactor", new Entry(tr("Реактор: управлять можно сигналами «назначить скорость расщепления» и «назначить выработку турбины» (0–100). ")
                    + tr("Выдаёт температуру, нагрузку, мощность и запас топлива."), List.of())),
            Map.entry("pump", new Entry(tr("Насос: «назначить скорость» от −100 (откачка) до 100 (накачка), «назначить целевой уровень» — уровень воды в процентах."), List.of())),
            Map.entry("engine", new Entry(tr("Двигатель: «назначить силу» от −100 до 100."), List.of())),
            Map.entry("door", new Entry(tr("Дверь: «переключить состояние» открывает и закрывает, «назначить состояние» — «1» открыть, «0» закрыть. Выдаёт своё состояние."), List.of())),
            Map.entry("powercontainer", new Entry(tr("Батарея / суперконденсатор: «установить скорость зарядки» 0–100; выдаёт заряд в процентах."), List.of())),
            Map.entry("turret", new Entry(tr("Орудие: «позиция_в» — куда целиться (с перископа или навигации), «вызвать_в» — выстрел."), List.of()))
    );

    private static final List<Circuit> CIRCUITS = List.of(
            new Circuit(tr("Автоматическая дверь"), tr("Дверь открывается, когда кто-то подходит, и закрывается сама."),
                    List.of(tr("Датчик движения поставьте рядом с дверью, Range X/Y — на ширину прохода."),
                            tr("Выход датчика «состояние_из» → вход двери «назначить состояние»."),
                            tr("Output датчика «1», False Output «0» — дверь закрывается, когда никого нет."))),
            new Circuit(tr("Тревога при затоплении"), tr("Сирена и красный свет, если в отсеке вода."),
                    List.of(tr("Датчик воды — у пола отсека."), tr("«сигнал_из» датчика → «назначить состояние» сирены и аварийной лампы."),
                            tr("Чтобы не срабатывало от лужи: «вода_%» → компонент «Больше» (вход 2 — порог через память, например «20») → сирена."))),
            new Circuit(tr("Свет мигает по тревоге"), tr("Лампа мигает, пока есть сигнал тревоги."),
                    List.of(tr("Генератор: Output Type — Pulse, Frequency — 1–2."), tr("Генератор → вход 1 компонента «И», сигнал тревоги → вход 2, Time Frame 0,1."),
                            tr("Выход «И» → «переключить состояние» лампы."))),
            new Circuit(tr("Кнопка-переключатель (вкл/выкл одним нажатием)"), tr("Одна кнопка включает устройство, повторное нажатие — выключает."),
                    List.of(tr("Кнопка → «переключить состояние» реле."), tr("Реле включено — сигнал или энергия проходят дальше; повторное нажатие выключает."),
                            tr("Сигнал для других устройств — с выхода реле «состояние_из»."))),
            new Circuit(tr("Задержка закрытия двери"), tr("Дверь остаётся открытой ещё несколько секунд."),
                    List.of(tr("Датчик движения → «Задержка» (Delay 3, Reset When Different Signal Received выкл.) → «назначить состояние» двери."),
                            tr("Или: датчик → «ИЛИ» вход 1 и → «Задержка» → «ИЛИ» вход 2; выход «ИЛИ» → дверь."))),
            new Circuit(tr("Реактор на автопилоте по нагрузке"), tr("Турбина следует за нагрузкой без инженера."),
                    List.of(tr("Выход реактора «значение нагрузки» → «Деление» вход 1; вход 2 — наибольшая мощность реактора (память, «5000»)."),
                            tr("Результат → «Умножение» на «100» → «назначить выработку турбины»."),
                            tr("Скорость деления: та же доля + запас: «Сложение» с «10» → «назначить скорость расщепления». Проверьте в игре, что температура в зелёной зоне."))),
            new Circuit(tr("Счётчик людей в отсеке на экране"), tr("Выводит текст на дисплей."),
                    List.of(tr("Датчик движения → «Склейка» (вход 1 — «В отсеке: », вход 2 — сигнал) → «назначить текст» дисплея."),
                            tr("Для чисел из датчиков (вода %, кислород %) то же самое: число → «Склейка» → дисплей.")))
    );

    private WiringGuide() {}

    /** Пояснение по тегу компонента из XML (AndComponent, MotionSensor, Pump…). */
    public static Optional<Entry> of(String component) { return Optional.ofNullable(ENTRIES.get(component.toLowerCase(Locale.ROOT))); }

    public static List<Circuit> circuits() { return CIRCUITS; }
}
