# Как устроен проект и как выпускается новая версия

Документ для быстрого восстановления контекста (для меня-помощника в новых сессиях и для вас).

## 1.4.86-fork (2026-08-20) — обычный Bits 'n' Bobs 2.2.x

Форк больше **не требует** GitHub-сборку `bits_n_bobs-2.0.2`. Работает с публичным
**Create: Bits 'n' Bobs 2.2.5** (CurseForge / Modrinth) и соседними последними
зависимостями 1.21.1 NeoForge:

| Мод | Было | Стало |
|---|---|---|
| Bits 'n' Bobs | GitHub 2.0.2 (кипти-форк) | **официальный 2.2.5** |
| Azimuth API | 1.4.2 | **1.4.7** |
| Strut Your Stuff | 1.2.7 | **1.3.0** |
| Sable | 2.0.3 | **2.0.5** |
| Create | 6.0.10 | 6.0.10 (актуальный релиз) |

Почему старый GitHub 2.0.2 был нужен: в BnB 0.0.44 не было API цепей 2.x.
В 2.2.x API то же (`com.kipti.bnb`), но появился гейт
`bits_n_bobs:dedicated_cogwheel_chain_component` — без тега катки не
принимаются в цепь (флаг UNDEDICATED по умолчанию выключен).

Что сделано:
- Все наши фланцевые/скрытые катки в тегах
  `dedicated_cogwheel_chain_component`, `flanged_cogwheel`,
  `extra_cogwheel_chain_candidates`.
- `BntFlangedCogwheelBlock` реализует `IFlangedCogWheel` (как ванильные
  фланцы BnB 2.2).
- Миксин `CogwheelChainCandidateMixin`: `isValidCandidate` всегда true
  для наших блоков (страховка, если тег не подхватился).
- `bits_n_bobs` в mods.toml теперь **required** `[2.2.0,)`.
- Лимит узлов цепи в `PlacingCogwheelChainMixin` читает
  `BnbConfigs.server().COGWHEEL_MAX_NODE_COUNT`.

## Что это

Форк мода **Create: Bits 'n' Tracks** (`bits_n_tracks`, NeoForge 1.21.1), восстановленный
декомпиляцией из `bits_n_tracks-1.0.3.1-release.jar` (лицензия GPL-3.0 — форк легален).
Группа пакетов: `dev.qwxon.bitsntracks`.

## Структура

```
bnt-fork/
├── build.sh                  ← сборка одной командой (см. ниже)
├── gradle.properties         ← версия мода: mod_version=1.3.9-fork
├── build.gradle              ← зависимости = локальные JAR из libs/
├── settings.gradle
├── libs/                     ← 9 JAR-зависимостей (скачиваются публично, см. список ниже)
└── src/main/
    ├── java/dev/qwxon/bitsntracks/
    │   ├── BitsNTracks.java           главный класс мода
    │   ├── content/CogAlignmentLeverItem.java   инструмент (5 режимов)
    │   ├── content/BntToolMode.java             режимы: ALIGN/GRIP/WIDTH/RADIUS/REST
    │   ├── physics/BntTrackSettings.java        константы: grip 25–300%, ширина
    │   │                                        0.875–2 блока, радиус 25–175%
    │   ├── physics/BntPhysicsEvents.java        физика: grip, жёсткие катки
    │   ├── physics/BntPhysicsRegistry.java      реестр «жёстких» (неактивных) катков
    │   ├── physics/CogwheelSizeHelper.java      радиусы/масштаб катков
    │   ├── physics/BntRadiusProvider.java       ThreadLocal для ленивого ребилда геометрии
    │   ├── client/BntChainWidthCache.java       кэш ширины ленты (оптимизация FPS)
    │   ├── client/BntRestTrackHelper.java       «лежачая гусеница» (тигр-режим)
    │   ├── mixin/KineticBlockEntityPhysicsMixin.java  NBT: BntGrip, BntTrackWidth,
    │   │                                            BntRadiusScale, BntRestTrack
    │   ├── mixin/CogwheelChain(Mixin/BehaviourMixin/...)  геометрия цепи/лента
    │   └── mixin/RotationPropagatorMixin.java   передача большая↔средняя/мелкая шестерня
    └── resources/
        ├── META-INF/neoforge.mods.toml          манифест (version= лотже дублируется!)
        ├── bits_n_tracks.mixins.json            список миксинов
        └── assets/bits_n_tracks/lang/           переводы en_us/ru_ru
```

## Зависимости (libs/) — откуда берутся

| Файл | Источник |
|---|---|
| create-1.21.1-6.0.10.jar | Modrinth `create` |
| flywheel-neoforge-1.21.1-1.0.6.jar | внутри create (`META-INF/jarjar/`) |
| ponder-neoforge-1.0.82+mc1.21.1.jar | внутри create (`META-INF/jarjar/`) |
| Registrate-MC1.21-1.3.0+67.jar | внутри create (`META-INF/jarjar/`) |
| azimuth-1.4.7.jar | CurseForge/Modrinth `azimuth-api` 1.4.7 |
| struts-1.3.0.jar | CurseForge/Modrinth `strut-your-stuff` 1.3.0 |
| sable-companion-common-*.jar | внутри struts (`META-INF/jarjar/`) |
| bits_n_bobs-2.2.5.jar | CurseForge/Modrinth **официальный** `create-bits-n-bobs` 2.2.5 |
| sable-neoforge-1.21.1-2.0.5.jar | CurseForge `sable` 2.0.5 (PolyForm Shield — НЕ встраивать) |

## Песочница: ограничения и обходы

- RAM ~2 ГБ, CPU 2 ядра. Этап `decompile` (vineflower) ест до ~2 ГБ → **обязателен swap**
  и снятие `memory.high` cgroup, иначе `createMinecraftArtifacts` вылетает молча
  (OOM-kill видно через `sudo dmesg | grep Killed`).
- JDK 21 качается в /tmp (не сохраняется между сессиями — build.sh перекачает сам).
- Первая сборка (с нулевым кэшем Gradle): ~11 минут. Дальнейшие: ~10 секунд.
- Сборка = `./gradlew jar`, результат в `build/libs/`.

## Чек-лист выпуска новой версии

1. Правки кода.
2. Поднять версию в **двух** местах: `gradle.properties` (`mod_version=`) и
   `src/main/resources/META-INF/neoforge.mods.toml` (`version= "..."`).
3. `bash build.sh`
4. Скопировать JAR в /home/user/ и отдать пользователю (present_file).
5. Обновить архив исходников `bnt-fork-source.zip` (src + gradle-файлы, БЕЗ libs,
   build, .gradle) — пользователь заливает его на GitHub как резервную копию.

## Открытые хвосты на момент 1.3.9+

- Фикс «торчащих верхушек» лежачей гусеницы (релиз 1.4.0) существовал в старой
  сессии, но в GitHub-исходники (1.3.9) НЕ попал → пере-применить: в
  `BntRestTrackHelper` для скрытых/физических катков использовать базовый
  ВИЗУАЛЬНЫЙ радиус (не уменьшенный collision-радиус).
- StackOverflowError у пользователя (бесконечная рекурсия
  RotationPropagator ⇄ CompactSpeedRegulatorBlockEntity из мода
  create_compact_transmission; краш на версии 1.3.7). Удалять наш
  RotationPropagatorMixin НЕЛЬЗЯ (просил пользователь) → делать
  re-entrancy-защиту отдельным миксином по имени класса (без зависимости от
  их мода).

## 1.4.4 (2026-08-01)
- **Stress config restored** (user request): BntServerConfig again has the [physics.stress]
  section with tiny/small/medium/largeStressImpact keys (range 0..4096, per-1-RPM impact,
  Create multiplies by speed). DEFAULTS ARE 0.0 — wheels do not load the kinetic network;
  pre-1.4.3 defaults were 2/4/6/8. BntStressValues provider now returns
  () -> BntServerConfig.getStressImpact(block). Old config files keep their saved values.
- **Phantom power from broken TFMG engines fixed**: engines whose structure was destroyed
  (the very state the crash left behind) kept generating rotation forever -> after world
  reload tracks spun "by themselves". Added bnt$canGenerateSpeedSafely to
  TfmgEngineSafetyMixin: replaces canGenerateSpeed()Z on AbstractSmallEngineBlockEntity
  (Regular engines inherit it; Radial/Turbine override with own gone-safe versions and are
  untouched). Now: own state must be SHAFT (original rule) AND every block of the engine
  row behind the shaft must still have the ENGINE_STATE property, else no output
  (getGeneratedSpeed -> 0 -> applyNewSpeed propagates stop). engineLength() = engines list
  size; row = pos.relative(shaftFacing.opposite(), i) for i=1..min(len,16).
- TFMG internals learned: GeneratingKineticBlockEntity.updateGeneratedRotation calls
  getGeneratedSpeed FIRST then calculateAddedStressCapacity (->hasTwoShafts); crash #3
  hit the latter because the controller block itself was intact. LargeEngine is a separate
  hierarchy (drives PoweredShaft) — not covered, not crashy in the same way.

## 1.4.6 (2026-08-02) — последний чужой миксин убран, форк 100% чистый
- Removed mixin/compat/CompactSpeedRegulatorReentryGuardMixin.java (protection against the
  RotationPropagator ⇄ create_compact_transmission recursion) + its mixins.json entry;
  mixin/compat/ is empty again. The protection NOW LIVES in tfmg-engine-fix 1.1.0
  (CompactRegulatorSafetyMixin + CompactSpeedLimiterSafetyMixin, ThreadLocal + input-speed
  cache). User MUST update both files together.
- mixins.json re-validated: 12 server mixins (all bits_n_tracks own) + 9 client, JSON valid.
- Search over src/: 0 matches for tfmg / drmangotea / lucse / compact — форк содержит
  ТОЛЬКО код Bits 'n' Tracks. Nothing else changed (stress config 0-defaults stay).

## 1.4.5 (2026-08-01) — TFMG logic MOVED OUT of the fork
- User decision: the fork must not touch TFMG at all. Removed:
  - mixin/compat/TfmgEngineSafetyMixin.java (deleted)
  - its entry in bits_n_tracks.mixins.json
  - build.gradle compileOnly tfmg line; libs/tfmg-1.2.0.jar moved to /home/user/tfmg-engine-fix
- Everything else identical to 1.4.4 (stress config with 0-defaults stays).
- The TFMG fixes (air-crash guard + phantom-power stop) now live in the separate
  mod project /home/user/tfmg-engine-fix → jar tfmg_engine_fix-1.0.0.jar
  (modid tfmg_engine_fix, mixin SmallEngineSafetyMixin, TFMG as compileOnly,
  optional dep in mods.toml — silently inert without TFMG installed).

## 1.4.7-fork (2026-08-02)
- Исправлено «самодвижение»: если колесо/гусеница осталось без сети Create
  (источник удалён, сеть распалась), но в NBT блока сохранилась старая
  скорость — тикающий обработчик теперь сбрасывает такую «призрачную»
  скорость в 0 на сервере. Без этого блоки визуально крутились вечно и
  вечно же толкали корпус (танк ехал сам, без двигателя).

## 1.4.8-fork (2026-08-02)
- Усилена защита от «самодвижения»: если колесо/гусеница имеет сеть, но в ней нет ни одного живого генерирующего источника (`!bnt$hasLiveSource`), скорость сбрасывается в 0 на сервере. Предотвращает вращение гусениц после разрушения валов привода.

## 1.4.9-fork (2026-08-02)
- Удалена проверка `!bnt$hasLiveSource` из `KineticBlockEntityPhysicsMixin`, которая в версии 1.4.8-fork приводила к ложному сбросу скорости (`setSpeed(0)`) на колёсах с активной сетью при запуске двигателей из TFMG и Create Diesel Generators, вызывая конфликт скоростей и разрушение моторов. Возвращена безопасная проверка `!self.hasNetwork()`, не вмешивающаяся в работу активных сетей Create.

## 1.4.10-fork (2026-08-02)
- В `RotationPropagatorMixin` добавлено устранение ложного зацепления зубьями (`-1.0F`) между соседними колёсами одной гусеничной цепи (`bnt$inSameChain`). В танковой гусенице колёса стоят вплотную и связаны цепью в одном направлении (`+1.0F`). Ранее ванильный Create считал их шестернями в зацеплении (`-1.0F`), вызывая конфликт вращения (`+1` против `-1`), мгновенно разрушавший любые подключённые двигатели (TFMG и Create Diesel Generators).

## 1.4.11-fork (2026-08-02)
- Добавлен универсальный миксин `GeneratingKineticBlockEntitySafetyMixin`, блокирующий разрушение любых двигателей Create (`level.destroyBlock`) при получении встречного/конфликтного вращения. Теперь ни двигатели TFMG, ни двигатели Create Diesel Generators, ни ванильные моторы Create никогда не ломаются и не выпадают предметом при подключении к гусеничным сетям Bits 'n' Tracks.

## 1.4.12-fork (2026-08-03)
- Добавлен миксин `RotationPropagatorSafetyMixin`: перехватывает все вызовы `level.destroyBlock` внутри `RotationPropagator.handleAdded()`. При подключении гусениц к коробкам передач (`create_compact_transmission`) и двигателям TFMG / Create Diesel Generators рекурсивный пересчёт сети вызывал разрушение блоков на уровне пропагатора вращения. Теперь любая попытка разрушить двигатель, коробку передач или колесо гусеницы из `RotationPropagator` блокируется.

## 1.4.13-fork (2026-08-03)
- В `RotationPropagatorSafetyMixin` защита от вызовов `level.destroyBlock` расширена на вообще ВСЕ кинетические блоки в игре (включая ванильные шестерни и валы Create, передающие вращение от двигателя к гусеницам). Ранее миксин блокировал разрушение только самих двигателей и коробок передач, из-за чего при изменении скорости (например, на творческом моторе или при переключении передач) ломалась промежуточная шестерня. Теперь ни один механизм в цепи привода не может сломаться от изменения скорости или направления.

## 1.4.14-fork (2026-08-03)
- Полный архитектурный рефакторинг по принципу мода Create Tracks+ (`create-tracks+`). Все глобальные миксины безопасности (`GeneratingKineticBlockEntitySafetyMixin`, `RotationPropagatorSafetyMixin`), влиявшие на остальные моды в игре, полностью удалены. В `RotationPropagatorMixin` колёса гусениц (`BntFlangedCogwheelBlock`, `HiddenCogwheelBlock`) больше не считаются шестернями (`ICogWheel`) при боковом зацеплении: они соединяются исключительно вдоль оси вала (`IRotate` -> `+1.0F`) или через гусеничную ленту/цепь (`CogwheelChainBehaviour`). Это полностью исключает конфликты направлений вращения (`+1` против `-1`), из-за которых ломались шестерни и моторы при изменении скорости.

## 1.4.15-fork (2026-08-03)
- Исправлена передача вращения между колёсами в одной гусеничной цепи (`bnt$inSameChain`). В версии 1.4.14-fork для колёс одной цепи в `RotationPropagator` возвращался `0.0F` (нет соединения), из-за чего крутилось только первое колесо, подключённое к мотору. Теперь возвращается `1.0F`, что объединяет все колёса гусеницы в единую кинетическую сеть, вращающуюся с одинаковой скоростью и направлением, сохраняя поведение Tracks+ (коробка с валом без бокового зубчатого зацепления с чужими шестернями).

## 1.4.16-fork (2026-08-03)
- Исправлена проверка принадлежности колёс к одной гусенице (`bnt$inSameChain`). В версии 1.4.15-fork поле `controlledChain` проверялось напрямую на каждом колесе, однако оно заполнено только на контроллере (мастер-блоке) гусеницы, из-за чего для остальных колёс проверка возвращала `false` и вращалось только первое колесо. Теперь проверка сравнивает позиции контроллеров (`bnt$getControllerPos`), что надёжно определяет колёса одной ленты и синхронно вращает всю гусеницу целиком.

## 1.4.17-fork (2026-08-04)
- Полностью удалена функция «лежачей гусеницы» (Resting track / Tiger-style return run) и все связанные с ней классы, методы и NBT-поля (`BntRestTrackHelper`, режим `REST` в ключе юстировки, NBT-тег `BntRestTrack`). Мод стал легче, чище и избавился от лишней логики отрисовки и физики.

## 1.4.18-fork (2026-08-04)
- Исправлен зазор в 1 пиксель между лентой гусеницы и нижней гранью катков. Ранее значения `trackRadius` по умолчанию были завышены на ~0.05-0.10 блока (`0.55` для малых, `0.35` для крошечных, `1.1` для больших), из-за чего лента отрисовывалась ниже визуального края колеса. Теперь значения по умолчанию точно соответствуют реальным радиусам катков (`0.25`, `0.49`, `0.73`, `0.99`), и лента плотно прилегает к ободу колёс любого размера.

## 1.4.19-fork (2026-08-04)
- Откачены настройки `trackRadius` из 1.4.18-fork обратно к значениям 1.4.17-fork (`0.35`, `0.55`, `0.74`, `1.1`), а также полностью возвращена функция «лежачей гусеницы» (Tiger-style return run, `BntRestTrackHelper`, режим `REST` в ключе юстировки). При этом в `BntRestTrackHelper` исправлен расчёт высоты верхней ленты над лежачими катками: теперь используется точное значение `BntRadiusProvider.getTrackRadius`, благодаря чему зазор сверху над катком абсолютно равен зазору снизу под катком (полная симметрия зазоров снизу и сверху).

## 1.4.20-fork (2026-08-04)
- В `BntRestTrackHelper` к высоте верхней ленты над лежачими катками добавлена небольшая прибавка (`+0.05` блока ≈ 0.8 пикселя), благодаря чему верхний зазор над колесом стал чуть больше, как и просил пользователь.

## 1.4.21-fork (2026-08-04)
- По просьбе пользователя нижний зазор в `BntPhysicsTuning` возвращён к дефолтам версии 1.4.17-fork / 1.4.16-fork (`0.35`, `0.55`, `0.74`, `1.1`), а прибавка `+0.05` из 1.4.20-fork убрана из расчёта верхней ленты. Теперь зазоры снизу под колесом и сверху над лежачим колесом строго идентичны и соответствуют стандартным значениям мода.

## 1.4.22-fork (2026-08-04)
- По просьбе пользователя верхний зазор над лежачими катками (`BntRestTrackHelper`) сделан чуть меньше (`wheelTopY = centerY + trackRadius - 0.03`). Лента гусеницы в режиме `REST` теперь ложится чуть ближе к ободу колеса.

## 1.4.23-fork (2026-08-04)
- По просьбе пользователя верхний зазор над лежачими катками (`BntRestTrackHelper`) сделан ещё чуть меньше (`wheelTopY = centerY + trackRadius - 0.05`). Верхняя лента в режиме `REST` теперь идеально ложится прямо на обод колеса без воздушного просвета.

## 1.4.24-fork (2026-08-04)
- По просьбе пользователя верхний зазор над лежачими катками (`BntRestTrackHelper`) уменьшен ещё на 0.015 блока (`wheelTopY = centerY + trackRadius - 0.065`). Верхняя лента в режиме `REST` теперь прилегает максимально близко и эстетично к ободу колеса.
