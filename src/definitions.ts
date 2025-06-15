export interface PIPPlugin {
  enter(options?: { width?: number; height?: number }): Promise<{ value: string }>;
  isPip(): Promise<{ value: boolean }>;
  autoPIP(options: { value: boolean; width?: number; height?: number }): Promise<{ value: string }>;
  aspectRatio(options: { width: number; height: number }): Promise<{ value: string }>;
  onPipModeChanged(): Promise<void>;
  isPipModeSupported(): Promise<{ value: boolean }>;
  addListener(eventName: 'onPipModeChanged', callback: (data: { value: boolean }) => void): Promise<void>;
}