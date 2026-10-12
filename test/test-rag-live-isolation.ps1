# Injected Docker command fake: verifies loopback/provenance guards with ZERO actual Docker/JDBC/provider calls.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot/rag-live-database.ps1"
$script:observed=@();$script:fake=@{};$script:fault=''
function Invoke-RagDocker {
    param([string[]]$DockerArgs,[int]$TimeoutSeconds=30)
    $script:observed+=,$DockerArgs
    switch($DockerArgs[0]){
        'info'{return 'MOCK'}
        'image'{return 'MOCK'}
        'run'{
            $script:fake=@{id=('a'*64);name=$DockerArgs[3];plan='';nonce=''}
            foreach($arg in $DockerArgs){if($arg.StartsWith('com.fpt.rag.plan=')){$script:fake.plan=$arg.Substring('com.fpt.rag.plan='.Length)};if($arg.StartsWith('com.fpt.rag.nonce=')){$script:fake.nonce=$arg.Substring('com.fpt.rag.nonce='.Length)}}
            return $script:fake.id
        }
        'inspect'{
            if($script:fault -eq 'identity'){return 'WRONG_ID'}
            if($script:fault -eq 'label'){return "$($script:fake.id)|false|$($script:fake.plan)|$($script:fake.nonce)|true"}
            return "$($script:fake.id)|true|$($script:fake.plan)|$($script:fake.nonce)|true"
        }
        'port'{if($script:fault -eq 'port'){return '0.0.0.0:5432'};return '127.0.0.1:45678'}
        'stop'{return 'MOCK'}
        'exec'{return 'MOCK'}
        'cp'{return 'MOCK'}
        default{throw 'Unexpected mock Docker operation'}
    }
}
$results=@()
foreach($fault in @('identity','label','port','none')){
    $script:fault=$fault;$script:observed=@();$blocked=$false;$context=$null
    try{$context=New-RagLiveTestDatabase -Root $root -PlanId ('offline-isolation-'+[guid]::NewGuid().ToString('N'))}catch{$blocked=$true}
    if($fault -eq 'none'){
        if($blocked -or !$context -or $context.Port -ne '45678' -or !$context.Nonce){throw 'Valid mock isolation rejected'}
        if(@($script:observed | Where-Object {$_[0] -eq 'exec' -and $_ -contains '-f'}).Count -ne 2){throw 'Expected both test migrations'}
    }else{
        if(!$blocked){throw "Unsafe mock target accepted: $fault"}
        if(@($script:observed | Where-Object {$_[0] -in @('exec','cp')}).Count){throw 'Database activity before isolation validation'}
    }
    if(@($script:observed | Where-Object {$_[0] -in @('rm','volume','prune')}).Count){throw 'Destructive Docker action'}
    $results+=@{case=$fault;result='PASS'};Write-Output "PASS container isolation $fault"
}
$report=Join-Path $root ('.build/phase241-isolation-'+[guid]::NewGuid().ToString('N')+'.json')
$file=[IO.File]::Open($report,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
try{$file.Write([Text.Encoding]::UTF8.GetBytes((@{passed=4;failed=0;actualDockerCommands=0;databaseAccess=0;realProviderRequests=0;cases=$results}|ConvertTo-Json -Depth 5)));$file.Flush($true)}finally{$file.Dispose()}
Write-Output "Isolation mocks: 4/4; sanitized result: $report"
