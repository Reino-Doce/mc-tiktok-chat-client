var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var LdcInsnNode = Java.type('org.objectweb.asm.tree.LdcInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

var FONT_HOOKS_OWNER = 'br/com/reinodoce/mctiktok/client/font/InlineMediaFontHooks';

function patchFontWidth(methodNode) {
    var continueLabel = new LabelNode();
    var patch = new InsnList();
    patch.add(new VarInsnNode(Opcodes.ILOAD, 1));
    patch.add(new MethodInsnNode(
        Opcodes.INVOKESTATIC,
        FONT_HOOKS_OWNER,
        'inlineAdvanceOrSentinel',
        '(I)F',
        false
    ));
    patch.add(new VarInsnNode(Opcodes.FSTORE, 3));
    patch.add(new VarInsnNode(Opcodes.FLOAD, 3));
    patch.add(new InsnNode(Opcodes.FCONST_0));
    patch.add(new InsnNode(Opcodes.FCMPG));
    patch.add(new JumpInsnNode(Opcodes.IFLT, continueLabel));
    patch.add(new VarInsnNode(Opcodes.FLOAD, 3));
    patch.add(new InsnNode(Opcodes.FRETURN));
    patch.add(continueLabel);

    methodNode.instructions.insert(patch);
    methodNode.maxLocals = Math.max(methodNode.maxLocals, 4);
    ASMAPI.log('INFO', 'Patched font-width => ' + methodNode.name + methodNode.desc);
    return methodNode;
}

function patchStringRenderOutputAccept(methodNode) {
    var continueLabel = new LabelNode();
    var patch = new InsnList();
    patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
    patch.add(new VarInsnNode(Opcodes.ILOAD, 1));
    patch.add(new VarInsnNode(Opcodes.ALOAD, 2));
    patch.add(new VarInsnNode(Opcodes.ILOAD, 3));
    patch.add(new MethodInsnNode(
        Opcodes.INVOKESTATIC,
        FONT_HOOKS_OWNER,
        'tryRenderInline',
        '(Ljava/lang/Object;ILnet/minecraft/network/chat/Style;I)Z',
        false
    ));
    patch.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
    patch.add(new InsnNode(Opcodes.ICONST_1));
    patch.add(new InsnNode(Opcodes.IRETURN));
    patch.add(continueLabel);

    methodNode.instructions.insert(patch);
    ASMAPI.log('INFO', 'Patched font-accept => ' + methodNode.name + methodNode.desc);
    return methodNode;
}

function initializeCoreMod() {
    return {
        'reinodoce_font_inline_media_width': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.gui.Font',
                'methodName': ASMAPI.mapMethod('m_243025_'),
                'methodDesc': '(ILnet/minecraft/network/chat/Style;)F'
            },
            'transformer': function(methodNode) {
                return patchFontWidth(methodNode);
            }
        },
        'reinodoce_font_inline_media_accept': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.gui.Font$StringRenderOutput',
                'methodName': ASMAPI.mapMethod('m_6411_'),
                'methodDesc': '(ILnet/minecraft/network/chat/Style;I)Z'
            },
            'transformer': function(methodNode) {
                return patchStringRenderOutputAccept(methodNode);
            }
        }
    };
}
