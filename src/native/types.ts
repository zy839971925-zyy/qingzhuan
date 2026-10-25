export type InputFile={id:string;name:string;file?:string;size:number;kind:string;tools:string[];audioTargets?:string[]};
export type OutputFile={id:string;name:string;size:number;time?:number};
export type Task={id:string;seq:number;kind:string;status:'idle'|'running'|'cancelling'|'complete'|'error'|'cancelled'|'interrupted';message:string;value:number;files?:OutputFile[]};
export type Snapshot={protocol:number;revision:number;inputs:InputFile[];outputs:OutputFile[];busy:boolean;task:Task;warning?:string};
export type UiState={theme:'system'|'light'|'dark';dark:boolean;reduced:boolean;powerSave:boolean};
export type Tool={id:string;title:string;formats:string[];description:string};
