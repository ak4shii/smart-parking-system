import { useState } from 'react';
import { X, Copy, Check, Eye, EyeOff, Shield, Server, Key, Hash } from 'lucide-react';
import type { MqttCredentials } from '../services/microcontrollerService';

interface MqttCredentialsDialogProps {
    credentials: MqttCredentials;
    onClose: () => void;
}

export default function MqttCredentialsDialog({ credentials, onClose }: MqttCredentialsDialogProps) {
    const [showPassword, setShowPassword] = useState(false);
    const [copiedFields, setCopiedFields] = useState<Record<string, boolean>>({});

    const copyToClipboard = async (text: string, field: string) => {
        try {
            await navigator.clipboard.writeText(text);
            setCopiedFields({ ...copiedFields, [field]: true });
            setTimeout(() => {
                setCopiedFields((prev) => ({ ...prev, [field]: false }));
            }, 2000);
        } catch (err) {
            console.error('Failed to copy:', err);
        }
    };

    const credentialFields = [
        {
            label: 'MQTT Host',
            value: credentials.mqttHost,
            field: 'host',
            icon: Server,
            copyable: true,
        },
        {
            label: 'MQTT Port',
            value: credentials.mqttPort.toString(),
            field: 'port',
            icon: Hash,
            copyable: true,
        },
        {
            label: 'Username',
            value: credentials.mqttUsername,
            field: 'username',
            icon: Key,
            copyable: true,
        },
        {
            label: 'Password',
            value: credentials.mqttPassword,
            field: 'password',
            icon: Shield,
            sensitive: true,
            copyable: true,
        },
        {
            label: 'Base Topic',
            value: credentials.baseTopic,
            field: 'topic',
            icon: Server,
            copyable: true,
        },
    ];

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
            <div className="w-full max-w-2xl rounded-3xl border border-slate-200 bg-white shadow-2xl">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 p-6">
                    <div>
                        <h3 className="text-xl font-bold text-slate-900">MQTT Credentials Generated</h3>
                        <p className="mt-1 text-sm text-slate-600">
                            Device: <span className="font-semibold text-slate-900">{credentials.deviceName}</span>
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        className="rounded-xl p-2 hover:bg-slate-100 transition-colors"
                        aria-label="Close"
                    >
                        <X className="h-5 w-5 text-slate-500" />
                    </button>
                </div>

                {/* Warning */}
                <div className="mx-6 mt-6 rounded-2xl border-2 border-amber-200 bg-amber-50 p-4">
                    <div className="flex gap-3">
                        <Shield className="h-5 w-5 flex-shrink-0 text-amber-600" />
                        <div>
                            <div className="text-sm font-semibold text-amber-900">Important Security Notice</div>
                            <div className="mt-1 text-sm text-amber-800">
                                The password is displayed{' '}
                                <span className="font-bold">only once</span> for security reasons.{' '}
                                <span className="font-semibold">Copy and store it securely now</span>. You won't be able to view it again.
                            </div>
                        </div>
                    </div>
                </div>

                {/* Credentials */}
                <div className="space-y-3 p-6">
                    {credentialFields.map((item) => {
                        const Icon = item.icon;
                        const isCopied = copiedFields[item.field];
                        const displayValue = item.sensitive && !showPassword ? '••••••••••••••••' : item.value;

                        return (
                            <div
                                key={item.field}
                                className="group rounded-2xl border border-slate-200 bg-slate-50 p-4 transition-all hover:border-indigo-200 hover:bg-indigo-50/50"
                            >
                                <div className="flex items-start justify-between gap-3">
                                    <div className="flex items-start gap-3 flex-1 min-w-0">
                                        <div className="mt-0.5 rounded-xl bg-white p-2 shadow-sm ring-1 ring-slate-200 group-hover:ring-indigo-200">
                                            <Icon className="h-4 w-4 text-slate-600 group-hover:text-indigo-600" />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                                                {item.label}
                                            </div>
                                            <div className="mt-1.5 font-mono text-sm text-slate-900 break-all">
                                                {displayValue}
                                            </div>
                                        </div>
                                    </div>

                                    <div className="flex items-center gap-2 flex-shrink-0">
                                        {item.sensitive && (
                                            <button
                                                onClick={() => setShowPassword(!showPassword)}
                                                className="rounded-lg p-2 text-slate-600 hover:bg-white hover:text-slate-900"
                                                aria-label={showPassword ? 'Hide password' : 'Show password'}
                                            >
                                                {showPassword ? (
                                                    <EyeOff className="h-4 w-4" />
                                                ) : (
                                                    <Eye className="h-4 w-4" />
                                                )}
                                            </button>
                                        )}

                                        {item.copyable && (
                                            <button
                                                onClick={() => copyToClipboard(item.value, item.field)}
                                                className={`rounded-lg p-2 transition-colors ${isCopied
                                                        ? 'bg-emerald-100 text-emerald-700'
                                                        : 'text-slate-600 hover:bg-white hover:text-slate-900'
                                                    }`}
                                                aria-label={isCopied ? 'Copied!' : 'Copy to clipboard'}
                                            >
                                                {isCopied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* Footer */}
                <div className="border-t border-slate-200 bg-slate-50 px-6 py-4 rounded-b-3xl">
                    <div className="flex items-center justify-between gap-4">
                        <div className="text-xs text-slate-600">
                            Use these credentials to configure your ESP32/IoT device
                        </div>
                        <button
                            onClick={onClose}
                            className="rounded-xl bg-indigo-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700 transition-colors"
                        >
                            Done
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
