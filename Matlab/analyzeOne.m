
javarmpath('C:\Users\janez\workspace\S2_rw\submodules\s2-java-lib\bin')
javarmpath('C:\Users\janez\workspace\S2_rw\submodules\pcardtimesync\bin')
javarmpath('C:\Users\janez\workspace\S2_rw\bin\')

javaaddpath('C:\Users\janez\workspace\S2_rw\submodules\s2-java-lib\bin')
javaaddpath('C:\Users\janez\workspace\S2_rw\submodules\pcardtimesync\bin')
javaaddpath('C:\Users\janez\workspace\S2_rw\bin\')
 
simpleS = javaObject('filters.Runner');
S = javaObject('e6.ECG.time_sync.Signal');    

%% Initial input parameters
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% CONSTANTS
nanos_in_one_hour = 3600e9;
colours = 'rgbrmkc';

folder = 'C:\Users\janez\workspace\S2_rw\S2files'
name = 'andrej1.s2'
    
%%%%%%%%%%%%%%%%%%%%%%   OLD TIMESTAMPS       %%%%%%%%%%%%%%55555
if 1
	simpleS.setOldTVP(name);
	stime1 = simpleS.getSamplesTimeStamp();
	stime1 = stime1;% - stime1(1);
	svolt1 = simpleS.getVoltage();
	speaks1 = simpleS.getPeaks();
	ptime1 = simpleS.getPacketsTimeStamp();
	ptime1 = ptime1 - ptime1(1);
	pcounter1 = simpleS.getPacketsCounter();
	%%ping1 = ptime1(2:end) - ptime1(1:end-1);
	%%jitter1 = abs(ping1(2:end) - ping1(1:end-1));
end    



%%%%%%%%%%%%%%%%%%5       NEW TIMESTAMPS         %%%%%%%%%%%%%%%%%%%%%%%%%%%
if 1
	simpleS.setNewTVP(name);
	stime2 = simpleS.getSamplesTimeStamp();
	stime2 = stime2;% .- stime2(1);
	svolt2 = simpleS.getVoltage();
	speaks2 = simpleS.getPeaks() + 1;
	ptime2 = simpleS.getPacketsTimeStamp();
	ptime2 = ptime2 .- ptime2(1);
	pcounter2 = simpleS.getPacketsCounter();  
end


%%%%%%%%%%%%%%%%%         ANDREJ TIMESTAMPS           %%%%%%%%%%%%%%%%%%%%%%
if 0
	int_length = 3;

	S.setIntervalLength(int_length);
	S.readS2File(folder, name, 0, 0.5 * nanos_in_one_hour, 0);
	S.processSignal;    

	%#  WARNING andrej meèe not dodatje sample

	stime3 = S.getSamplesTimeStamp; 
	stime3 = stime3 .- stime3(1);
	svolt3 = S.getVoltage;
	speaks3 = S.getPeaks + 1; % +1, because Matlab counting starts from 1
	ptime3 = S.getNewTimeStamp;  
	ptime3 = ptime3 .- ptime3(1);
	pcounter3 = S.getCounter;
end  


  
%%%%%%%%%%%%%%%%%         SECOND FILE           %%%%%%%%%%%%%%%%%%%%%%
if 1
	simpleS.setNewTVP("andrej2.s2");
	stime4 = simpleS.getSamplesTimeStamp();
	stime4 = stime4;% .- stime4(1);
	svolt4 = simpleS.getVoltage();
	speaks4 = simpleS.getPeaks() + 1;
	ptime4 = simpleS.getPacketsTimeStamp();
	ptime4 = ptime4 .- ptime4(1);
	pcounter4 = simpleS.getPacketsCounter();  
end

 
  


%%%%%%%%%%%%%%%           calculate PING/JITTER         %%%%%%%%%%%%%%%%%
if 0
	ping2 = zeros(size(ptime2)-1,1);
	for i =2:size(ptime2);
		 if (pcounter2(i) - pcounter2(i-1) == 14)
			  ping2(i-1) = ptime2(i) - ptime2(i-1); 
		 end  
	end
	jitter2 = zeros(size(ptime2)-2,1);
	for i =2:size(ping2);
		 if ((pcounter2(i+1) - pcounter2(i) == 14) && (pcounter2(i) - pcounter2(i-1) == 14))
			  jitter2(i-1) = ping2(i) - ping2(i-1); 
		 end  
	end  


	ping3 = zeros(size(ptime3)-1,1);
	for i =2:size(ptime3);
		 if (pcounter3(i) - pcounter3(i-1) == 14)
			  ping3(i-1) = ptime3(i) - ptime3(i-1); 
		 end  
	end
	jitter3 = zeros(size(ptime3)-2,1);
	for i =2:size(ping3);
		 if ((pcounter3(i+1) - pcounter3(i) == 14) && (pcounter3(i) - pcounter3(i-1) == 14))
			  jitter3(i-1) = ping3(i) - ping3(i-1); 
		 end  
	end  
end  

  
% PRIMERJAVA vrhov med andrej1.s2 in andrej2.s2
% andrej1 je "pravi" andrej2 pa ocenjujemo glede na podobnost s pravim andrej1
if 0
	vsotaPreh = 0;
	vsotaZamuj = 0;
	niPrimerjave = 0;
	j = -10;
	k = 10;
	ii = 1
	ni = true;
	najdeni = zeros(size(speaks2),1);
	for(i = 1:size(speaks2))
		for iii = j:k
			if (ii+iii<1 | ii+iii>size(speaks4))
				continue
			end
			aaa = stime2(speaks2(i)) - stime4(speaks4(ii+iii));
			if abs(aaa) < 2*10^8
				if aaa>0
					vsotaZamuj = vsotaZamuj + aaa;
				else
					vsotaPreh = vsotaPreh + aaa;
					
				end
				najdeni(i) = aaa;
				ii = i+1;
				ni = false;
				break
			end
		end  
		if ni
			niPrimerjave = niPrimerjave + 1;
			ii = ii + 1;
			najdeni(i) = 5*10^7;
			ni = true;
		end
	end  
end
  
  
  
  
%%%%%%%%%%%%%%%%%%%%%%%%%         PLOT             %%%%%%%%%%%%%%%%%%%%%%%%%%  
  
close('all') 

%%%%  PRMERJAVA VRHOV

if 0
	
	figure;
	hold on;
	plot(stime2(speaks2),najdeni)
	
	niPrimerjave
end	
  
%%%%  NAPETOS

if 0
	figure;
	hold on; 
	a = 10*10^9;
	zamik = 0.3*10^9; %% sekunde
	b = a + zamik;
	plot(stime2(stime2>a & stime2<b),svolt2(stime2>a & stime2<b),'-g+')
end
if 1
	figure;
	hold on; 
	a = 17*10^9;
	zamik = 10*10^9; %% sekunde
	b = a + zamik;
	%d = 1299;
	  
	  
	  
	  %riši nad èasom
	if 1
		vz = 0.3;  %%za lepši izris
		%% 1
		plot(stime1(stime1>a & stime1<b),svolt1(stime1>a & stime1<b),'-r')
		BB = stime1(speaks1);
		CC = BB(BB>a & BB<b);
		DD = svolt1(speaks1);
		EE = DD(BB>a & BB<b);
		plot(CC,EE,'r*')
		%% 2	
		plot(stime2(stime2>a & stime2<b),svolt2(stime2>a & stime2<b)+vz,'-g')
		BB = stime2(speaks2);
		CC = BB(BB>a & BB<b);
		DD = svolt2(speaks2);
		EE = DD(BB>a & BB<b);
		plot(CC,EE+2*vz,'g*')
		%% 3
		%plot(stime3(a:b),svolt3(a:b)./100,'-b')
		%% 4
		plot(stime4(stime4>a & stime4<b),svolt4(stime4>a & stime4<b)-vz,'-m')
		BB = stime4(speaks4);
		CC = BB(BB>a & BB<b);
		DD = svolt4(speaks4);
		EE = DD(BB>a & BB<b);
		plot(CC,EE+2*vz,'m*')
		 
		xlabel('time [ns]')
		%riši nad prvih nekaj paketi
	else
		%plot(a:b,svolt1(a:b),'-r')  
		plot(a:b,svolt2(a:b),'-g')
		plot(a:b,svolt4(a:b),'-m')
		 
		 
		 
		plot(tpeaks2,svolt2(tpeaks2),'g*')
		plot(tpeaks4,svolt4(tpeaks4),'m*')
		 
		xlabel('paketki')
	end
end


%%%%%%  COUNTER in TIME
%%razen lokalno neuporabno
if 0
  figure;
  hold on; 
  plot(ptime1,pcounter1,'r.','markers',20)
  plot(ptime2,pcounter2,'g.','markers',14)
end

if 0
  figure;
  hold on; 
  plot(pcounter1./14,ptime1,'r.','markers',20)
  plot(pcounter2./14,ptime2,'g.','markers',14)
  plot(pcounter3./14,ptime3,'b.','markers',8)
  xlabel('counter')
  ylabel('time [ns]')
end

%%%%   PING - razlika timestampov od paketa do paketa (hitrost)
if 0
  figure;
  hold on;
  %plot(1:size(ping1),ping1,'r')
  plot(1:size(ping2),ping2,'g')
  plot(1:size(ping3),ping3,'b')
end


%%%%    JITTER - razlika ping med tremi paketi (pospešek)
if 0
  figure;
  hold on;
  %plot(1:size(jitter1),jitter1,'r')
  plot(ptime2(2:end-1),jitter2,'g')
  %plot(,jitter2>10)
  plot(ptime3(2:end-1),jitter3,'b')
  xlabel("counter")
  ylabel("jitter [ns]")
end






%%%%%%%%%%%%%%       IZPIS statiske simpleS         %%%%%%%%%%%%%
if 0
  "izpis simple"
  prviPaketTime = ptime2(1)
  zadnjiPaketTime = ptime2(end)
  slabih = sum(ping2 > (2*10^8))
  normalnih = sum(ping2>(10^8))
  paketov = size(ping2)(1)
  razmerje = normalnih/paketov
  lomi = sum(abs(jitter2)>15)
end



%%%%%%%%%%%%%%       IZPIS statiske S         %%%%%%%%%%%%%
if 0
  "izpis signal"
  prviPaketTime = ptime3(1)
  zadnjiPaketTime = ptime3(end)
  slabih = sum(ping3 > (2*10^8))
  normalnih = sum(ping3>(10^8))
  paketov = size(ping3)(1)
  razmerje = normalnih/paketov
  lomi = sum(abs(jitter3)>15)
end




%%%%%%%%%%%%%%       izpis statiske PRIMERJAVE        %%%%%%%%%%%%%

if 0
    "izpis primerjava"
    simplePeaks = size(speaks2)(1)
    signalPeaks = size(speaks4)(1)
    if (size(speaks2) == size(speaks4))
        enakih = sum(speaks2 == speaks3)
    end
end




'DONE'