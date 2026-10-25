import {NativeEventEmitter,NativeModules} from 'react-native';
import type {Snapshot,UiState} from './types';
const nativeModule=NativeModules.Qingzhuan;
if(!nativeModule)throw new Error('Qingzhuan Java module is unavailable');
export const events=new NativeEventEmitter(nativeModule);
export const native={
 initialUi:JSON.parse(nativeModule.initialUi) as UiState,
 sync:async():Promise<Snapshot>=>JSON.parse(await nativeModule.sync()),
 ui:async():Promise<UiState>=>JSON.parse(await nativeModule.getUi()),
 pick:()=>nativeModule.pick(),
 ready:()=>nativeModule.startupReady(),
 start:(id:string,tool:string,options:Record<string,unknown>,ids:string[])=>nativeModule.startTask(id,tool,JSON.stringify(options),JSON.stringify(ids)),
 cancel:(id:string)=>nativeModule.cancelTask(id),
 remove:(id:string)=>nativeModule.removeInput(id),clear:()=>nativeModule.clearInputs(),
 save:(id:string)=>nativeModule.save(id),open:(id:string)=>nativeModule.open(id),share:(id:string)=>nativeModule.share(id),
 saveSelected:(ids:string[])=>nativeModule.saveSelected(JSON.stringify(ids)),deleteSelected:(ids:string[])=>nativeModule.deleteSelected(JSON.stringify(ids)),removeSelected:(ids:string[])=>nativeModule.removeSelected(JSON.stringify(ids)),
 saveAll:(ids:string[])=>nativeModule.saveAll(JSON.stringify(ids)),deleteAll:()=>nativeModule.deleteAll(),
 theme:(mode:UiState['theme'])=>nativeModule.setTheme(mode),haptic:()=>nativeModule.haptic(),licenses:():Promise<string>=>nativeModule.licenses(),
};
