#!/bin/bash
# ============================================
#  Сборка форка Bits 'n' Tracks (NeoForge 1.21.1)
#  Использование:  bash build.sh
#  Скрипт сам подготовит окружение (JDK 21, swap,
#  лимиты памяти) и соберёт JAR в build/libs/
# ============================================
set -e
cd "$(dirname "$0")"

# --- 1. JDK 21 ---
if [ ! -x /tmp/jdk21/bin/java ]; then
  echo ">>> Скачиваю JDK 21 (Temurin)..."
  wget -q -O /tmp/jdk21.tar.gz "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
  mkdir -p /tmp/jdk21
  tar xzf /tmp/jdk21.tar.gz -C /tmp/jdk21 --strip-components=1
  rm -f /tmp/jdk21.tar.gz
fi
export JAVA_HOME=/tmp/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
echo ">>> $(java -version 2>&1 | head -1)"

# --- 2. Swap (в этом песочнице всего ~2 ГБ RAM,
#        декомпилятору Minecraft нужно ~2+ ГБ) ---
if ! swapon --show=NAME --noheadings 2>/dev/null | grep -q swapfile; then
  echo ">>> Поднимаю swap 8 ГБ..."
  for i in 1 2; do
    f="/swapfile$i"
    [ -f "$f" ] || sudo fallocate -l 4G "$f"
    sudo chmod 600 "$f"
    sudo /sbin/mkswap "$f" > /dev/null 2>&1 || true
    sudo /sbin/swapon "$f" 2>/dev/null || true
  done
fi
# --- 3. Лимиты памяти cgroup (иначе OOM на этапе decompile) ---
echo max | sudo tee /sys/fs/cgroup/user/memory.high > /dev/null 2>&1 || true
echo 5153960755 | sudo tee /sys/fs/cgroup/user/memory.max > /dev/null 2>&1 || true

# --- 4. Сборка ---
export JAVA_TOOL_OPTIONS="-Xmx2000m"
chmod +x gradlew
./gradlew jar --console=plain --no-daemon -Dorg.gradle.workers.max=1 "$@"

echo ""
echo ">>> Готово! JAR лежит здесь:"
ls -la build/libs/
