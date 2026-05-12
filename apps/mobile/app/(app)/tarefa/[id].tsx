import React, { useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  TextInput,
  Alert,
  TouchableOpacity,
} from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTarefa, useEntregaveis, useComentarios, useAdicionarComentario } from '@/lib/queries';
import { uploadEntregavel, FileTooLargeError } from '@/lib/upload';
import { Spinner } from '@/components/ui/Spinner';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Card } from '@/components/ui/Card';
import { colors, spacing, typography, radius } from '@/lib/theme';

export default function TarefaDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data: tarefa, isLoading } = useTarefa(id);
  const { data: entregaveis, refetch: refetchEntregaveis } = useEntregaveis(id);
  const { data: comentarios } = useComentarios(id);
  const adicionarComentario = useAdicionarComentario(id);

  const [comentarioText, setComentarioText] = useState('');
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  if (isLoading || !tarefa) return <Spinner />;

  async function handlePickAndUpload() {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.All,
      quality: 0.8,
    });
    if (result.canceled) return;

    const asset = result.assets[0];
    setUploading(true);
    setUploadProgress(0);
    try {
      await uploadEntregavel(
        id,
        asset.uri,
        asset.mimeType ?? 'application/octet-stream',
        asset.fileName ?? 'upload',
      );
      await refetchEntregaveis();
      Alert.alert('Sucesso', 'Material enviado com sucesso!');
    } catch (err) {
      if (err instanceof FileTooLargeError) {
        Alert.alert('Arquivo muito grande', err.message);
      } else {
        Alert.alert('Erro no upload', (err as Error).message);
      }
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  }

  async function handleCameraCapture() {
    const perm = await ImagePicker.requestCameraPermissionsAsync();
    if (perm.status !== 'granted') {
      Alert.alert('Permissão negada', 'Ative a câmera nas configurações do dispositivo.');
      return;
    }
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.All,
      quality: 0.8,
    });
    if (result.canceled) return;

    const asset = result.assets[0];
    setUploading(true);
    try {
      await uploadEntregavel(
        id,
        asset.uri,
        asset.mimeType ?? 'application/octet-stream',
        asset.fileName ?? 'captura',
      );
      await refetchEntregaveis();
      Alert.alert('Sucesso', 'Material enviado!');
    } catch (err) {
      Alert.alert('Erro no upload', (err as Error).message);
    } finally {
      setUploading(false);
    }
  }

  async function handleComentario() {
    if (!comentarioText.trim()) return;
    try {
      await adicionarComentario.mutateAsync(comentarioText.trim());
      setComentarioText('');
    } catch (err) {
      Alert.alert('Erro', 'Não foi possível enviar o comentário');
    }
  }

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <TouchableOpacity onPress={() => router.back()} style={styles.back} accessibilityRole="button">
        <Text style={styles.backText}>← Voltar</Text>
      </TouchableOpacity>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.titulo}>{tarefa.titulo}</Text>
        <View style={styles.badges}>
          <Badge label={tarefa.status} variant="outline" />
          <Badge
            label={tarefa.prioridade}
            variant={tarefa.prioridade === 'ALTA' || tarefa.prioridade === 'CRITICA' ? 'destructive' : 'warning'}
          />
        </View>

        {tarefa.descricao && <Text style={styles.descricao}>{tarefa.descricao}</Text>}
        {tarefa.prazo && (
          <Text style={styles.prazo}>
            Prazo: {new Date(tarefa.prazo).toLocaleDateString('pt-BR')}
          </Text>
        )}

        {/* Entregáveis */}
        <Text style={styles.sectionTitle}>Materiais enviados</Text>
        {entregaveis?.length === 0 && (
          <Text style={styles.empty}>Nenhum material enviado ainda.</Text>
        )}
        {entregaveis?.map((e) => (
          <Card key={e.id} style={styles.entregavelCard}>
            <Text style={styles.entregavelName} numberOfLines={1}>{e.filename}</Text>
            <Badge label={e.status} variant={e.status === 'APROVADO' ? 'success' : 'default'} />
            {e.feedback && <Text style={styles.feedback}>{e.feedback}</Text>}
          </Card>
        ))}

        <View style={styles.uploadActions}>
          <Button
            onPress={handlePickAndUpload}
            label="Galeria"
            variant="outline"
            loading={uploading}
            style={styles.uploadBtn}
            testID="btn-galeria"
          />
          <Button
            onPress={handleCameraCapture}
            label="Câmera"
            variant="primary"
            loading={uploading}
            style={styles.uploadBtn}
            testID="btn-camera"
          />
        </View>

        {/* Comentários */}
        <Text style={styles.sectionTitle}>Comentários</Text>
        {comentarios?.map((c) => (
          <Card key={c.id} style={styles.comentCard}>
            <Text style={styles.comentAutor}>
              {c.autorTipo === 'CREATOR' ? 'Você' : 'Assessora'}
            </Text>
            <Text style={styles.comentText}>{c.texto}</Text>
          </Card>
        ))}

        <View style={styles.comentInput}>
          <TextInput
            style={styles.input}
            placeholder="Adicionar comentário..."
            placeholderTextColor={colors.muted}
            value={comentarioText}
            onChangeText={setComentarioText}
            multiline
            testID="input-comentario"
            accessibilityLabel="Comentário"
          />
          <Button
            onPress={handleComentario}
            label="Enviar"
            loading={adicionarComentario.isPending}
            disabled={!comentarioText.trim()}
            testID="btn-comentario"
          />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  back: { padding: spacing.lg, paddingBottom: spacing.sm },
  backText: { color: colors.primary, fontSize: typography.size.base },
  content: { padding: spacing.lg, paddingTop: 0, gap: spacing.sm },
  titulo: {
    fontSize: typography.size['2xl'],
    fontWeight: typography.weight.bold,
    color: colors.ink,
  },
  badges: { flexDirection: 'row', gap: spacing.sm, flexWrap: 'wrap' },
  descricao: { color: colors.muted, fontSize: typography.size.base, lineHeight: 24 },
  prazo: { color: colors.muted, fontSize: typography.size.sm },
  sectionTitle: {
    fontSize: typography.size.lg,
    fontWeight: typography.weight.semibold,
    color: colors.ink,
    marginTop: spacing.lg,
  },
  empty: { color: colors.muted, fontSize: typography.size.sm },
  entregavelCard: { gap: spacing.xs },
  entregavelName: { fontSize: typography.size.sm, color: colors.ink },
  feedback: { fontSize: typography.size.sm, color: colors.muted, fontStyle: 'italic' },
  uploadActions: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.md },
  uploadBtn: { flex: 1 },
  comentCard: { gap: spacing.xs },
  comentAutor: { fontSize: typography.size.xs, fontWeight: typography.weight.semibold, color: colors.muted },
  comentText: { fontSize: typography.size.sm, color: colors.ink },
  comentInput: { gap: spacing.sm, marginTop: spacing.md },
  input: {
    minHeight: 80,
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    fontSize: typography.size.base,
    color: colors.ink,
    textAlignVertical: 'top',
  },
});
