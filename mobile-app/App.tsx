import { useState } from 'react';
import {
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  useColorScheme,
} from 'react-native';
import { StatusBar } from 'expo-status-bar';

export default function App() {
  const scheme = useColorScheme();
  const isDark = scheme === 'dark';
  const palette = isDark ? darkPalette : lightPalette;
  const [taps, setTaps] = useState(0);

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: palette.bg }]}>
      <StatusBar style={isDark ? 'light' : 'dark'} />
      <View style={styles.container}>
        <Text style={[styles.eyebrow, { color: palette.muted }]}>ThePlace</Text>
        <Text style={[styles.title, { color: palette.fg }]}>
          L'app que tout le monde attend.
        </Text>
        <Text style={[styles.subtitle, { color: palette.muted }]}>
          Squelette Expo + React Native + TypeScript. Prêt à itérer.
        </Text>

        <Pressable
          onPress={() => setTaps((n) => n + 1)}
          style={({ pressed }) => [
            styles.cta,
            {
              backgroundColor: palette.accent,
              opacity: pressed ? 0.85 : 1,
            },
          ]}
        >
          <Text style={[styles.ctaLabel, { color: palette.accentFg }]}>
            {taps === 0 ? 'Commencer' : `Tapé ${taps} fois`}
          </Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const lightPalette = {
  bg: '#fafafa',
  fg: '#0a0a0a',
  muted: '#6b7280',
  accent: '#0a0a0a',
  accentFg: '#ffffff',
};

const darkPalette = {
  bg: '#0a0a0a',
  fg: '#fafafa',
  muted: '#a1a1aa',
  accent: '#fafafa',
  accentFg: '#0a0a0a',
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,
  },
  container: {
    flex: 1,
    paddingHorizontal: 28,
    justifyContent: 'center',
    gap: 12,
  },
  eyebrow: {
    fontSize: 13,
    fontWeight: '600',
    letterSpacing: 2,
    textTransform: 'uppercase',
  },
  title: {
    fontSize: 36,
    fontWeight: '700',
    lineHeight: 42,
  },
  subtitle: {
    fontSize: 16,
    lineHeight: 22,
    marginBottom: 24,
  },
  cta: {
    alignSelf: 'flex-start',
    paddingHorizontal: 22,
    paddingVertical: 14,
    borderRadius: 999,
  },
  ctaLabel: {
    fontSize: 16,
    fontWeight: '600',
  },
});
