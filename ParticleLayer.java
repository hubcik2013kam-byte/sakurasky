package pl.sakurasky;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;

/**
 * Wlasna warstwa czasteczek: symulacja + jeden draw call z teksturowanymi billboardami.
 * Rendering wg przykladu docs.fabricmc.net/1.21.11/develop/rendering/world (z dodana tekstura).
 */
public class ParticleLayer {
	private static final RenderPipeline PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath(SakuraSkyClient.MOD_ID, "pipeline/particles"))
					.withVertexShader("core/position_tex_color")
					.withFragmentShader("core/position_tex_color")
					.withSampler("Sampler0")
					.withBlend(BlendFunction.TRANSLUCENT)
					.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
					.withDepthWrite(false)
					.withCull(false)
					.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
					.build());

	private static final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
	private MappableRingBuffer vertexBuffer;

	private static class P {
		double x, y, z;
		float vx, vy, vz;
		float age, life, rot, spin, phase;
	}

	private final List<P> particles = new ArrayList<>();
	private final Random rnd = new Random();
	private long lastNanos = 0;
	private ParticleKind lastKind = null;

	public void render(WorldRenderContext context) {
		Config cfg = Config.I;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) { particles.clear(); return; }
		if (!cfg.particlesEnabled) { particles.clear(); return; }

		ParticleKind kind = ParticleKind.of(cfg.particleType);
		if (kind != lastKind) { particles.clear(); lastKind = kind; }

		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
		lastNanos = now;
		double time = now / 1.0e9;

		Vec3 cam = context.worldState().cameraRenderState.pos;
		simulate(kind, cfg, cam, dt, time);
		if (particles.isEmpty()) return;

		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		double yaw = Math.toRadians(mc.player.getViewYRot(partial));
		double pitch = Math.toRadians(mc.player.getViewXRot(partial));
		double s = Math.sin(yaw), c = Math.cos(yaw), st = Math.sin(pitch), ct = Math.cos(pitch);
		Vector3f right = new Vector3f((float) -c, 0f, (float) -s);
		Vector3f up = new Vector3f((float) (-s * st), (float) ct, (float) (c * st));

		float cr = ((cfg.particleColor >> 16) & 255) / 255f;
		float cg = ((cfg.particleColor >> 8) & 255) / 255f;
		float cb = (cfg.particleColor & 255) / 255f;

		PoseStack matrices = context.matrices();
		matrices.pushPose();
		matrices.translate(-cam.x, -cam.y, -cam.z);
		Matrix4fc m = matrices.last().pose();

		BufferBuilder buf = new BufferBuilder(allocator, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
		float h = cfg.particleSize / 2f;
		if (kind == ParticleKind.SPARK) h *= 0.6f;
		if (kind == ParticleKind.DUST) h *= 0.5f;
		if (kind == ParticleKind.FIREFLY) h *= 1.4f;

		for (P p : particles) {
			float fade = Math.min(1f, Math.min(p.age / 0.8f, (p.life - p.age) / 1.2f));
			if (fade <= 0f) continue;
			float a = fade;
			if (kind == ParticleKind.FIREFLY) a *= 0.55f + 0.45f * (float) Math.sin(time * 3 + p.phase);
			if (kind == ParticleKind.DUST) a *= 0.5f;

			float cs = (float) Math.cos(p.rot), sn = (float) Math.sin(p.rot);
			// obrocone osie billboardu
			float rx = right.x * cs + up.x * sn, ry = right.y * cs + up.y * sn, rz = right.z * cs + up.z * sn;
			float ux = -right.x * sn + up.x * cs, uy = -right.y * sn + up.y * cs, uz = -right.z * sn + up.z * cs;

			v(buf, m, p, rx, ry, rz, ux, uy, uz, -h, -h, 0f, 1f, cr, cg, cb, a);
			v(buf, m, p, rx, ry, rz, ux, uy, uz, h, -h, 1f, 1f, cr, cg, cb, a);
			v(buf, m, p, rx, ry, rz, ux, uy, uz, h, h, 1f, 0f, cr, cg, cb, a);
			v(buf, m, p, rx, ry, rz, ux, uy, uz, -h, h, 0f, 0f, cr, cg, cb, a);
		}
		matrices.popPose();

		draw(mc, buf, Identifier.fromNamespaceAndPath(SakuraSkyClient.MOD_ID, "textures/particle/" + kind.texture + ".png"));
	}

	private static void v(BufferBuilder b, Matrix4fc m, P p, float rx, float ry, float rz, float ux, float uy, float uz,
						  float dx, float dy, float u, float vv, float r, float g, float bl, float a) {
		b.addVertex(m, (float) (p.x + rx * dx + ux * dy), (float) (p.y + ry * dx + uy * dy), (float) (p.z + rz * dx + uz * dy))
				.setUv(u, vv).setColor(r, g, bl, a);
	}

	private void simulate(ParticleKind kind, Config cfg, Vec3 cam, float dt, double time) {
		// ruch + usuwanie
		for (int i = particles.size() - 1; i >= 0; i--) {
			P p = particles.get(i);
			p.age += dt;
			float fall = cfg.fallSpeed;
			switch (kind) {
				case SAKURA -> {
					p.vx = (float) Math.sin(time * 0.9 + p.phase) * 0.9f;
					p.vz = (float) Math.cos(time * 0.7 + p.phase) * 0.6f;
					p.vy = -0.9f * fall;
					p.rot += p.spin * dt;
				}
				case SNOW -> {
					p.vx = (float) Math.sin(time * 0.6 + p.phase) * 0.35f;
					p.vz = (float) Math.cos(time * 0.5 + p.phase) * 0.35f;
					p.vy = -1.4f * fall;
				}
				case FIREFLY -> {
					p.vx = (float) Math.sin(time * 0.8 + p.phase) * 0.5f;
					p.vz = (float) Math.cos(time * 0.6 + p.phase * 1.3) * 0.5f;
					p.vy = (float) Math.sin(time * 0.5 + p.phase * 2) * 0.3f;
				}
				case SPARK -> {
					p.vy = 2.5f * (1.4f - fall * 0.4f);
					p.vx *= 0.99f; p.vz *= 0.99f;
				}
				case DUST -> {
					p.vx = (float) Math.sin(time * 0.3 + p.phase) * 0.15f;
					p.vz = (float) Math.cos(time * 0.25 + p.phase) * 0.15f;
					p.vy = -0.08f * fall;
				}
			}
			p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
			if (p.age >= p.life || p.y < cam.y - 12 || p.y > cam.y + 24
					|| Math.abs(p.x - cam.x) > 32 || Math.abs(p.z - cam.z) > 32) {
				particles.remove(i);
			}
		}
		// spawn (max ~ count/4 na klatke, zeby nie bylo falowania)
		int target = Math.max(0, cfg.particleCount);
		while (particles.size() > target) particles.remove(particles.size() - 1);
		int spawn = Math.min(target - particles.size(), Math.max(4, target / 4));
		for (int i = 0; i < spawn; i++) {
			P p = new P();
			p.x = cam.x + (rnd.nextDouble() * 2 - 1) * 24;
			p.z = cam.z + (rnd.nextDouble() * 2 - 1) * 24;
			p.phase = rnd.nextFloat() * 6.28f;
			p.rot = rnd.nextFloat() * 6.28f;
			p.spin = (rnd.nextFloat() - 0.5f) * 3f;
			p.age = 0;
			switch (kind) {
				case SAKURA, SNOW -> { p.y = cam.y + 6 + rnd.nextDouble() * 12; p.life = 20f; }
				case FIREFLY -> { p.y = cam.y - 2 + rnd.nextDouble() * 8; p.life = 8f + rnd.nextFloat() * 8f; }
				case SPARK -> {
					p.y = cam.y - 2 + rnd.nextDouble() * 4; p.life = 2f + rnd.nextFloat() * 2f;
					p.vx = (rnd.nextFloat() - 0.5f) * 1.5f; p.vz = (rnd.nextFloat() - 0.5f) * 1.5f;
				}
				case DUST -> { p.y = cam.y - 3 + rnd.nextDouble() * 12; p.life = 14f; }
			}
			// nowe czasteczki "rozsiane" po calym zycii, zeby nie spadaly jedna chmura
			if (particles.size() < target / 2) p.age = rnd.nextFloat() * p.life * 0.5f;
			particles.add(p);
		}
	}

	private void draw(Minecraft client, BufferBuilder buffer, Identifier texId) {
		MeshData built = buffer.buildOrThrow();
		MeshData.DrawState params = built.drawState();
		VertexFormat format = params.format();

		int size = params.vertexCount() * format.getVertexSize();
		if (vertexBuffer == null || vertexBuffer.size() < size) {
			if (vertexBuffer != null) vertexBuffer.close();
			vertexBuffer = new MappableRingBuffer(() -> SakuraSkyClient.MOD_ID + " particles",
					GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
		}
		CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView view = enc.mapBuffer(
				vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
			MemoryUtil.memCopy(built.vertexBuffer(), view.data());
		}
		GpuBuffer vertices = vertexBuffer.currentBuffer();

		built.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
		GpuBuffer indices = PIPELINE.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
		VertexFormat.IndexType indexType = built.drawState().indexType();

		AbstractTexture tex = client.getTextureManager().getTexture(texId);
		GpuBufferSlice dyn = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> SakuraSkyClient.MOD_ID + " particle pass",
				client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
				client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", dyn);
			pass.bindTexture("Sampler0", tex.getTextureView(), tex.getSampler());
			pass.setVertexBuffer(0, vertices);
			pass.setIndexBuffer(indices, indexType);
			pass.drawIndexed(0, 0, params.indexCount(), 1);
		}
		built.close();
		vertexBuffer.rotate();
	}

	public void close() {
		allocator.close();
		if (vertexBuffer != null) { vertexBuffer.close(); vertexBuffer = null; }
	}
}
